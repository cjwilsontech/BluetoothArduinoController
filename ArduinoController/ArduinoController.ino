// Set constants.
const int MOTOR_LEFT = 3;
const int MOTOR_RIGHT = 11;
const int DIRECTION_LEFT = 12;
const int DIRECTION_RIGHT = 13;
enum SIDE {
  LEFT,
  RIGHT
};

// Controls the state of a motor.
void setMotor(SIDE side, int speed, bool forwards = true) {
  switch (side) {

    case LEFT:
      analogWrite(MOTOR_LEFT, speed);
      digitalWrite(DIRECTION_LEFT, (forwards) ? LOW : HIGH);
      break;

    case RIGHT:
      analogWrite(MOTOR_RIGHT, speed);
      digitalWrite(DIRECTION_RIGHT, (forwards) ? HIGH : LOW);
      break;

  }
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  pinMode(MOTOR_LEFT, OUTPUT);
  pinMode(MOTOR_RIGHT, OUTPUT);
  pinMode(DIRECTION_LEFT, OUTPUT);
  pinMode(DIRECTION_RIGHT, OUTPUT);
}

void loop() {
  // put your main code here, to run repeatedly:
  if (Serial.available()) {
      // Get the data packet.
      byte data = (byte)Serial.read();

      // Extract the data from the packet.
      // Format: SSSSSSDM
      // S = Speed
      // D = Direction (forwards = 1, backwards = 0)
      // M = Motor (left = 0, right = 1)
      
      SIDE side = ((data & 1) == 0) ? LEFT : RIGHT;
      bool forwards = (data & 2) == 2;
      int speed = (data & 252);
      
      // Adjust speed for the lost accuracy.
      if (speed != 0) speed += 3;
      
      // Make the changes.
      setMotor(side, speed, forwards);
  }
}

