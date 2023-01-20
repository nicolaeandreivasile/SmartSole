
#include <iostream>
#include <string.h>

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <WiFi.h>

#include "FS.h"
#include "SPIFFS.h"

#include "WiFi.h"
#include "time.h"


///////////////////////////////////////////////////////////////////////////////
//                                                                           //
//                               BLE FUNCTIONS                               //
//                                                                           //
///////////////////////////////////////////////////////////////////////////////

#define DATA_REQ        0x10

#define SERVICE_UUID                    "c5e15750-62b8-4e9b-acb0-dc94739b6bbd"
#define CHARACTERISTIC_NOTIFY_IF_UUID   "86aa2651-5a92-4265-a676-4d77ec3354e7"
#define CHARACTERISTIC_NOTIFY_EF_UUID   "3e40cd36-6dc6-474b-8cbd-740c7774852a"
#define CHARACTERISTIC_NOTIFY_B_UUID    "cca5bd11-3448-4b42-b359-49307e084248"

int valueIF;
int valueEF;
int valueB;

BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristicNotifyIF = NULL;
BLECharacteristic *pCharacteristicNotifyEF = NULL;
BLECharacteristic *pCharacteristicNotifyB = NULL;

bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  };

  void onDisconnect(BLEServer* pServer) {
    BLEDevice::startAdvertising();
    deviceConnected = false;
  }
};

void bleSetup() {
  // Create the BLE device
  BLEDevice::init("SmartSole ESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE service 
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  // Create BLE characteristics
  pCharacteristicNotifyIF = pService->createCharacteristic(
                        CHARACTERISTIC_NOTIFY_IF_UUID, 
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
  pCharacteristicNotifyEF = pService->createCharacteristic(
                        CHARACTERISTIC_NOTIFY_EF_UUID, 
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
  pCharacteristicNotifyB = pService->createCharacteristic(
                        CHARACTERISTIC_NOTIFY_B_UUID, 
                        BLECharacteristic::PROPERTY_NOTIFY
                      );
  
  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0); // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
}


///////////////////////////////////////////////////////////////////////////////
//                                                                           //
//                            SENSOR FUNCTIONS                               //
//                                                                           //
///////////////////////////////////////////////////////////////////////////////

#define IF_IN     25
#define EF_IN     26
#define B_IN      27


void computeReads() {
  valueIF = analogRead(IF_IN);
  valueEF = analogRead(EF_IN);
  valueB = analogRead(B_IN);

  valueIF = (valueIF * 100) / sum;
  valueEF = (valueEF * 100) / sum;
  valueB = (valueB * 100) / sum;
}

///////////////////////////////////////////////////////////////////////////////
//                                                                           //
//                            ARDUINO FUNCTIONS                              //
//                                                                           //
///////////////////////////////////////////////////////////////////////////////

void setup() {
  Serial.begin(115200);

  // Setup BLE atributes
  bleSetup();
}

void loop() {
  if (deviceConnected) {
      delay(2000); // send data every 2 seconds
      
      computeReads();
      
      pCharacteristicNotifyIF->setValue(valueIF);
      pCharacteristicNotifyIF->notify();
      pCharacteristicNotifyEF->setValue(valueEF);
      pCharacteristicNotifyEF->notify();
      pCharacteristicNotifyB->setValue(valueB);
      pCharacteristicNotifyB->notify();
  }
}
