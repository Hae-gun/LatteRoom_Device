void setup() {
  Serial.begin(9600);
  pinMode(10,OUTPUT);
}

void loop() {
  if(Serial.available()){
    String s = Serial.readStringUntil('\n');
    int lightPower = s.toInt();
    
    analogWrite(10,lightPower);
    
    }
}
