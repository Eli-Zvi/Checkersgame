## There are three steps for using the system:
### 1. Extracting the files from this folder
### 2. Starting the server
### 3. Starting as many clients as wanted or needed

### These three steps are explained in detail down below

## Start by extracting the folders from CheckersGame.zip

## HOW TO START THE SERVER

### Method 1 - Using Docker:

##### bash or shell
> Remove -d flag to keep the container attached and view its logs
```
cd Server
docker-compose up -d
```

### Method 2 - Manual with MySQL:

### Start the MySQL Service
#### WINDOWS:
##### shell
> Replace mysql_service_name with the name of the mysql service
```shell
net start mysql_service_name
```

#### LINUX:
##### bash
```bash
sudo service mysql_service_name start
```

### Start the Server

#### WINDOWS/LINUX:
##### bash or shell
```bash
cd Server
java -jar Checkers-Server.jar
```

## HOW TO START THE CLIENT

### WINDOWS:

##### shell
```shell
cd Client
start Checkers-Client.exe
```

or simply go to the client folder and double-click the file named Checkers-Client.exe!

### LINUX:
##### bash
```bash
cd Client
chmod +x Checkers-Client
./Checkers-Client
```

### If Method 1 was used, to delete all images and volumes created by docker:

### bash or shell

```
docker-compose down --volumes --rmi all
```