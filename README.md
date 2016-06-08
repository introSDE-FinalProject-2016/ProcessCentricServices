# ProcessCentricServices

The Process Centric Service is a RestFul Web Service. This module serves all requests coming directly from [User Interface](https://github.com/introSDE-FinalProject-2016/Telegram-Bot) layer and redirects them to [Business Logic Services](https://github.com/introSDE-FinalProject-2016/BusinessLogicServices) or [Storage Services](https://github.com/introSDE-FinalProject-2016/StorageServices) modules.

[API Documentation](http://docs.processcentricservices.apiary.io/#)  
[URL Client](https://github.com/introSDE-FinalProject-2016/Telegram-Bot)  
[URL Server (heroku)](https://desolate-thicket-56593.herokuapp.com/sdelab/processCentric-service) 


###Install
In order to execute this server locally you need the following technologies:

* Java (jdk 1.8.0)
* Ant (version 1.9.6)

Then, clone the repository. Run in your terminal:

```
$ git clone like https://github.com/introSDE-FinalProject-2016/ProcessCentricServices.git && cd ProcessCentricServices
```

and run the following command:
```
ant install
```

###Getting Started
To run the server locally then run:
```
ant start
```
