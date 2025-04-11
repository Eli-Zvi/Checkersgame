## Start by extracting the folders from CheckersGame.zip

## HOW TO START THE SERVER

### Method 1 - Using Docker:

##### bash or shell
> Remove -d flag to keep the container attached and view its logs
```bash
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

### LINUX:
##### bash
```bash
cd Client
chmod +x Checkers-Client
./Checkers-Client
```