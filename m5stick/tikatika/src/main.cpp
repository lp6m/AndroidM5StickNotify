
#include <M5GFX.h>
M5GFX display;

#include <M5StickCPlus.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

BLEUUID SERVICE_UUID = BLEUUID::fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");

#define CHARACTERISTIC_IMAGE_UUID "ba9aecfc-d4c7-4688-b67d-bf76ab618105"
#define CHARACTERISTIC_TEXT_UUID "beb5483f-36e1-4688-b7f5-ea07361b26a8"

#define ICON_DRAW_SIZE 64
#define STATUS_POS_X 128
#define LED_ONLY_COMMAND "LED_ONLY_COMMAND"
#define CLEAR_COMMAND "CLEAR_COMMAND"
#define LCD_BRIGHTNESS 200
#define LED_ON_DURATION 8000

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
BLECharacteristic* pCharacteristic2 = NULL;
bool deviceConnected = false;
int led_on_count = LED_ON_DURATION;

void clearNotification(){
  //clear image
  display.fillRect(0, 0, ICON_DRAW_SIZE, ICON_DRAW_SIZE, BLACK);
  //clear notification
  display.fillRect(0, ICON_DRAW_SIZE, display.width(), display.height() - ICON_DRAW_SIZE, BLACK);
}
void updateStatus(std::string status_str){
  display.setCursor(STATUS_POS_X, 0);
  display.fillRect(STATUS_POS_X, 0, display.width() - STATUS_POS_X, 64, BLACK);
  display.print(status_str.c_str());
}

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      updateStatus("connected");
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      updateStatus("disconnected");
      deviceConnected = false;
    }
};

class SendTextCallBack: public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic *pCharacteristic) {
  }

  void onWrite(BLECharacteristic *pCharacteristic) {
    std::string value = pCharacteristic->getValue();
    if (value == LED_ONLY_COMMAND){
      display.sleep();
      display.setBrightness(0);
      led_on_count = 0;
      digitalWrite(M5_LED, LOW); //turn on
    } else if (value == CLEAR_COMMAND) {
      clearNotification();
    } else {
      display.wakeup();
      display.setBrightness(LCD_BRIGHTNESS);
      clearNotification();
      display.setCursor(0, 64);
      display.print(value.c_str());
    }
  }
};


void drawIcon(int offset_x, int offset_y, int width, int height, unsigned short* img_ptr){
  for( int y = 0 ; y < ICON_DRAW_SIZE ; y++ ){
    for( int x = 0 ; x < ICON_DRAW_SIZE ; x++ ){
      int yy = y/4;
      int xx = x/4;
      display.drawPixel(offset_x + x , offset_x + y , img_ptr[yy*16+xx]);
    }
  }
}

class SendImageCallBack: public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic *pCharacteristic) {
  }

  void onWrite(BLECharacteristic *pCharacteristic) {
    uint8_t* value = pCharacteristic->getData();
    drawIcon(0, 0, 32, 32, (unsigned short*)value);
  }
};


void initBLEServise() {
  BLEDevice::init("M5Stack");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID, (uint32_t) 60, (uint8_t) 0);
  pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_TEXT_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                        //  BLECharacteristic::PROPERTY_NOTIFY |
                                        //  BLECharacteristic::PROPERTY_INDICATE
                                       );
  pCharacteristic->setCallbacks(new SendTextCallBack());
  // pCharacteristic->addDescriptor(new BLE2902());

  pCharacteristic2 = pService->createCharacteristic(
                                         CHARACTERISTIC_IMAGE_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );
                                        //  BLECharacteristic::PROPERTY_NOTIFY |
                                        //  BLECharacteristic::PROPERTY_INDICATE
  pCharacteristic2->setCallbacks(new SendImageCallBack());
  // pCharacteristic2->addDescriptor(new BLE2902());

  pService->start();
  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->start();
}


void setup() {
  M5.begin();
  initBLEServise();
  display.begin();
  display.setBrightness(LCD_BRIGHTNESS);
  display.setTextSize(1);
  display.setRotation(3);
  display.setTextWrap(true);
  display.setFont(&lgfxJapanMincho_16);
  pinMode(M5_LED, OUTPUT);
  digitalWrite(M5_LED, HIGH);
  updateStatus("initialized");
}

void loop() {
  M5.update();

  if(M5.BtnA.wasPressed()){
    display.setCursor(0, 64);
    display.print("Pressed A button.");
  }

  if(M5.BtnB.wasPressed()){
    esp_restart();
  }

  if (led_on_count < LED_ON_DURATION) {
    led_on_count++;
    if (led_on_count == LED_ON_DURATION){
      digitalWrite(M5_LED, HIGH); //turn off
    }
  }
}
