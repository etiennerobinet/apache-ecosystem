## Docker solution for IoTDB-Grafana Monitoring

This Dockerfile can be used to start a Docker container with a IoTDB server and a Grafana instance connected to it.

## Setup

1. Launch the `start.bat` to create the image and start the container
2. Open your browser and go to http://localhost:3000 to connect to Grafana
3. Use following credentials:
   - Username :`admin`
   - Password: `admin`
4. Grafana will then prompt you to change the password to a new one
5. To add IoTDB click on <img src="/media/add.png"/> 
6.  Select the `SimpleJson` DataSource type <img src="/media/json.png"/> 

7. Enter a **Name** for your DataSource and the URL (default is http://127.0.0.1:8880) <img src="\media\iotdb.png"/>  
