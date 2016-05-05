NOTICE 
=========

 **This project was done with little knowledge on Scala and playframework! Some parts of the code should not be taken as best practice  examples or examples at all. Refactoring  is planned.
 I've done this just for fun. **
 
 Thank you for reading!       


Spirit-Play
=================================

##What is Spirit?
 Spirit-News is a simple blogging system for the Faculty of Computer Science of the University of Applied Sciences Schmalkalden!             

##Features
 * News will be directly posted on Twitter and Facebook
 * responsive design
 * using playframework, Java 8 and Elasticsearch
 * personal schedule stored in localstorage of the browser
 * ical export to import to your favorite calendar application
 * schedule will updated dayly 
 
#Quickstart
 **NOTICE**: You have to install elasticsearch and start the daemon.
 The fowllowing command will create the Database:

```shell
    curl -XPUT 'http://localhost:9200/spirit-play'
```

For development you need jdk8 or higher. 
```shell
 cd /path/to/repo
 ./init.sh
 ./bin/activator
 ~run
```
This will download all librarys and start the application in dev mode.
For IDE setup take a look [here](https://playframework.com/documentation/latest/IDE).
