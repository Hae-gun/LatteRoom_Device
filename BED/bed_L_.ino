#include <Servo.h> 
 
int servoPinL = 9;
int servoPinR = 10;

Servo servo;

void setup()  
{
    analogReference(INTERNAL);
    Serial.begin(9600);
    pinMode(12,OUTPUT);
    pinMode(13,OUTPUT);
    servo.attach(servoPinL);
    servo.attach(servoPinR);
}

void loop()  
{
//    reading = analogRead(lm35Pin);
//    temperature = reading / 9.31;
//    
//    Serial.println(temperature);
//    delay(1000);

//    
if( Serial.available() ) {
String s = Serial.readStringUntil('\n');
  int first = s.indexOf(",");
  int second = s.indexOf(",",first+1);
  int lth = s.length();

  String str1 = s.substring(0,first);
  String str2 = s.substring(second+1,lth);
  
  if(str2=="0"){
    servo.write(90);
    }else if(s=="45"){
      servo.write(45);
      }else if(s=="90"){
        servo.write(0);
        }
//     Serial.println(s);
//
//      if(s.substring(0,2).equals("ON")){
//        digitalWrite(13,HIGH);
//      }
//      if( s.substring(0,2).equals("OFF") ){
//        digitalWrite(13,LOW);
//      }


//  if(s=="15"){
//    for(int i=90; i>75; --i){
//  servo.write(i);
//  delay(50);}}
//  if(s=="30"){
//    for(int i=90;i>60; --i){
//  servo.write(i);
//  delay(50);}}
//  if(s=="45"){
//    for(int i=90; i>45; --i){
//  servo.write(i);
//  delay(50);}}
//  if(s=="60"){
//    for(int i=90; i>30; --i){
//  servo.write(i);
//  delay(50);}}
//  if (s=="75"){
//    for(int i=90; i>15; --i){
//  servo.write(i);
//  delay(50);}}
//  if(s=="90"){
//for(int i=90; i>0; --i){
//  servo.write(i);
//  delay(50);}
//    
//  }
//  delay(1000);
//  servo.write(90);
//  delay(1000);
    }
    
}
