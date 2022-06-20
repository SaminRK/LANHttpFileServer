# Simple LAN HTTP File Server
___
### A simple HTTP File Server that can be used to transfer and host files in a LAN. There is also a Client that can be used to upload files to the server directory using the terminal. Files in the root directory of the server can be accessed through a browser from any device in the LAN. Files can also be uploaded.
This was part of our course-work from CSE322: Computer Networks Sessional at CSE, BUET. The purpose of this assignment was to teach us how to write programs in the application layer over the Transport Layer using the Socket library in Java. I had to reverse-engineer many of the HTTP requests and response from the browser in order to make my application compatible when used with browser. The final solution is very hackish and obviously this is not the way to design an HTTP File Server. But since I had fun working on this assignment, I am making the repository public.
___

**Java Version: Java 8 (1.8.0_312)**

### How to run on Ubuntu
1. Make sure you have Java JDK >= 8 installed. Here is the installation guide. [https://openjdk.org/install/](https://openjdk.org/install/)
2. Clone the repository
    ```bash
    git clone https://github.com/SaminRK/LANHttpFileServer.git
    ```
3. Then compile and run the Server using the following commands.
   ```bash
      javac src/SimpleHTTPServer.java
      java src/SimpleHTTPServer
   ```
4. From the browser open [localhost:8080](localhost:8080/)
5. A root directory will automatically be formed. You can manually add files and folder from our server machine into the root directory to make it accessible to other devices in the LAN.   
### Shortfalls
There are still areas that I need to work on. I will come back to them later (hoperfully).
1. Files can only be uploaded to the root directory.
2. Cannot create folders or directories from the browser.
3. Making a customisable config and logging system.
4. Separate out code for Client and Server.
5. Have not tested Client application at the time of writing this README.