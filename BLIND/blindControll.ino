#include <Servo.h>



Servo myservo;

int on = 180;
int off = 0;
int turn = 90;
String blindState="OFF";

void setup() {

  myservo.attach(6);
  Serial.begin(9600);
}

void loop() {

 if(Serial.available()){
    String state = Serial.readStringUntil('\n');
    
    if(state=="ON"&&blindState=="OFF"){
      
     blindState="ON";
      myservo.write(180);
      delay(3000);
      Serial.println(blindState);
    }else if(state=="OFF"&&blindState=="ON"){
       blindState="OFF";
       myservo.write(0);
       delay(3000);
       Serial.println(blindState);
    }
  }
   
    
   
   turn = 90;
  myservo.write(turn);


}
