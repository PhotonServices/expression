curl -X POST -H "Content-Type: application/json" -d '{
  "commands": [
    "1 new >>> Test Card",
    "2 cmt:Test-Card >>> Qué buena está la comida.",
    "3 cmt:Test-Card >>> El servicio me gustó.",
    "4 cmt:Test-Card >>> Los baños están asquerosos :(.",
    "5 cmt:Test-Card >>> Las meseras están bien bonitas hehe.",
    "6 cmt:Test-Card >>> Voy a volver varias veces con mucho gusto."
  ]
}' http://localhost:9000/demoer
