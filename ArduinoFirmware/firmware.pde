#include <NewSoftSerial.h>

int bluetoothTx = 2;
int bluetoothRx = 3;
int interval;
int prevTime;
int currentTime;
int count;

float randNumber;

NewSoftSerial bluetooth(bluetoothTx, bluetoothRx);

void setup()
{
  //Setup usb serial connection to computer
  Serial.begin(9600);
  Serial.print("AndroidBTCom...");
  
  //Setup Bluetooth serial connection to android
  bluetooth.begin(57600);
  bluetooth.print("$$$");
  delay(100);
  bluetooth.println("U,9600,N");
  bluetooth.begin(9600);
  
  interval = 500;
  prevTime = 0;
  currentTime = 0;
  count = 0;
  
  randomSeed(analogRead(0));
}

void loop()
{
  //Read from bluetooth and write to usb serial
  //if(bluetooth.available())
  //{
  //  char toSend = (char)bluetooth.read();
  //  Serial.print(toSend);
  //}

  //Read from usb serial to bluetooth
  //if(Serial.available())
  //{
  //  char toSend = (char)Serial.read();
  //  bluetooth.print(toSend);
  //}
  
  currentTime = millis();
  if ( (currentTime-prevTime) % interval == 0)
  {
   
    randNumber = random(300);
    randNumber += 0.5;
    prevTime = currentTime;
    bluetooth.println(randNumber);
    Serial.println(randNumber);
    count++;
  }
  
}
