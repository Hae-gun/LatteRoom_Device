#include "DHT.h"

#define DHTPIN_1 2      // DHT핀을 2번으로 정의한다(DATA핀)

#define LM35PIN A0

#define DHTTYPE DHT11  // DHT타입을 DHT11로 정의한다

DHT dht1(DHTPIN_1, DHTTYPE);  // DHT설정 - dht (디지털2, dht11)


int reading;
float lm35Temp;


void setup() {

  analogReference(INTERNAL);
  dht1.begin();
 
  Serial.begin(9600);    // 9600 속도로 시리얼 통신을 시작한다

}

 

void loop() {
  if( Serial.available() ) {
    String s = Serial.readStringUntil('\n');
    
    if(s=="COOLON"){
      digitalWrite(13,250);
    } else if(s=="COOLOFF") {
      digitalWrite(13,LOW);
    }
    
    if( s=="HEATON"){
      digitalWrite(12,250);
    }else if(s=="HEATOFF") {
      digitalWrite(12,LOW);
    }
  }

//  delay(100);

  //int h1 = dht1.readHumidity();  // 변수 h에 습도 값을 저장 
  int h1 = dht1.readHumidity();
  
  //int t1 = dht1.readTemperature();  // 변수 t에 온도 값을 저장
 // int t2 = dht2.readTemperature();

  reading = analogRead(LM35PIN);
  lm35Temp = ((reading/9.31)-4);
  
  
  //Serial.print("Humidity: ");  // 문자열 Humidity: 를 출력한다.
  
    // 변수 h(습도)를 출력한다.
  
 
  
  //Serial.print("%\t");  // %를 출력한다
  
  //Serial.print("Temperature: ");  // 이하생략
  
  //Serial.print(t1);
  
  //Serial.print(" C | ");
  
  //Serial.print(t2);
  
  //Serial.print(" C | ");
  Serial.print(h1);
  Serial.print(",");
  
  
  //Serial.print("LM35 Temp: ");  // 이하생략
  
  Serial.println(lm35Temp);

  delay(500);

  //Serial.println("\n");
  //Serial.println(" C");

//  lm35Temp

}
