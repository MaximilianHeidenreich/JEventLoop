<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPLv3 License][license-shield]][license-url]

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/MaximilianHeidenreich/JEventLoop.svg?style=flat-square
[contributors-url]: https://github.com/MaximilianHeidenreich/JEventLoop/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/MaximilianHeidenreich/JEventLoop?style=flat-square
[forks-url]: https://github.com/MaximilianHeidenreich/JEventLoop/network
[stars-shield]: https://img.shields.io/github/stars/MaximilianHeidenreich/JEventLoop?style=flat-square
[stars-url]: https://github.com/MaximilianHeidenreich/JEventLoop/stargazers
[issues-shield]: https://img.shields.io/github/issues/MaximilianHeidenreich/JEventLoop?style=flat-square
[issues-url]: https://github.com/MaximilianHeidenreich/JEventLoop/issues
[license-shield]: https://img.shields.io/github/license/MaximilianHeidenreich/JEventLoop?style=flat-square
[license-url]: https://github.com/MaximilianHeidenreich/JEventLoop/blob/master/LICENSE

<!-- PROJECT HEADER -->
<br />
<p align="center">
  <a href="https://github.com/MaximilianHeidenreich/JEventLoop">
    <img src="https://github.com/MaximilianHeidenreich/JEventLoop/blob/master/assets/Icon-128.png?raw=true" alt="Project Logo" >
  </a>

  <h2 align="center">JEventLoop</h2>

  <p align="center">
    A zero* dependency EventLoop for async Java applications like servers.
    <br>
    <small>zero* - I use <a href="https://logging.apache.org/log4j/2.x/">Log4J</a> for logging purposes</small>
    <br />
    <a href="#"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/MaximilianHeidenreich/JEventLoop/issues">Report Bug</a>
    ·
    <a href="https://github.com/MaximilianHeidenreich/JEventLoop/issues">Request Feature</a>
  </p>
</p>

<!-- TABLE OF CONTENTS -->
## Table of Contents

- [Table of Contents](#table-of-contents)
- [About The Project](#about-the-project)
  - [Features](#features)
- [Usage](#usage)
- [Benchmark](#benchmark)
- [Contributing](#contributing)
- [Contact](#contact)

<!-- ABOUT THE PROJECT -->
## About The Project

I created this project because I needed it for my [JNet]() and [EnderSync]() projects. 
Everything else I found online might have worked but would result in too much boilerplate code or
just had an API that was pretty big.
I just wanted a simple EventLoop without* any other dependencies which could process my events.

### Features

- [x] Events
    - [x] Queue events
    - [x] Handle events with multiple custom handler functions
    - [x] Prioritize specific events if necessary
- [x] Callbacks
    - [x] Queue events with callbacks which can be executed from inside event handlers.

<br>

<!-- USAGE -->
## Usage

#### Add the dependency to your pom.xml
```xml
<project>
    ...
    <repositories>
        <repository>
            <id>maximilianheidenreich</id>
            <name>GitHub MaximilianHeidenreich Apache Maven Packages</name>
            <url>https://maven.pkg.github.com/maximilianheidenreich/*</url>
        </repository>
    </repositories>
    ...
    <dependencies>
        <dependency>
            <groupId>de.maximilianheidenreich</groupId>
            <artifactId>jeventloop</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
    ....
</project>
```

#### Creating a custom Event

Custom events can be used to execute specific handlers for specific events. Custom events can store any data 
or logic you want them to. Just extends the default Event class and implement anything you need.

**⚠️ Make sure to call `super()` inside any custom constructor! Otherwise, your event won't be handled!**

```java
import de.maximilianheidenreich.jeventloop.events.Event;

class MyCoolEvent extends Event {
  
    // You can store custom data associated with your event
    public String message;
    
    // You can use 
    public MyCoolEvent(String message) {
        super();    // ! Call super() ! Otherwise the event is corrupted and will be ignored !
        this.message = message;
    }
    
    // You can add custom logic to your event
    public void print() {
        System.out.println(this.message);
    }
    
}
```

#### Setup an EventLoop

```java
import de.maximilianheidenreich.jeventloop.EventLoop;

class Main {
    public static void main(String[] args) {

        EventLoop eventLoop = new EventLoop();
        
        // EITHER: Starts the event loop as a separate thread -> Non-Blocking
        eventLoop.start();
        
        // OR: Run the event loop in this thread -> Blocking
        eventLoop.run();

    }
}
```

#### Register event handlers

You can register as many as you need, and they will be executed in order *(first registered -> first executed)*.

**⚠️ Make sure any code you call inside your handlers is thread safe!**

```java
import de.maximilianheidenreich.jeventloop.EventHandle;
import de.maximilianheidenreich.jeventloop.EventLoop;

class Main {
    public static void main(String[] args) {

        // ...

        // You can add a simple lambda function for small handlers
        eventLoop.addEventHandler(MyAwesomeEvent.class, (handle) -> {
            MyAwesomeEvent event = (MyAwesomeEvent) handle.getEvent();
            // Do some work
        });

        // You can add a function reference for bigger tasks
        eventLoop.addEventHandler(MyAwesomeEvent.class, Main::bigHandleLogic);

    }

    public static void bigHandleLogic(EventHandle handle) {
        MyAwesomeEvent event = (MyAwesomeEvent) handle.getEvent();

        // Do some work e.g. process data
        String data = event.message.toUpperCase();
        
        // If you want to break the chain of execution (stop executing handlers registered after this one) call:
        handle.cancel();
      
        // If there was an exception, just throw it!
        // It will get handled, logged and other handlers ill continue to execute!
        throw new Exception("Oops, something went wrong!");
      
        // Optionally call callbacks when the data is ready
        handle.callback(data);
    }
}
```

#### Queue an Event

**⚠️ Make sure any code you call inside your callbacks is thread safe!**

```java
import de.maximilianheidenreich.jeventloop.EventLoop;
class Main {
    public static void main(String[] args) {

        EventLoop eventLoop = new EventLoop();
        //...
        
        // Queue an event and ignore any callbacks
        eventLoop.queueEvent(new MyAwesomeEvent("Hello World"));
        
        // Queue an event and register a callback
        eventLoop.queueEvent(new MyAwesomeEvent("Hello Async World"))
            .thenApply((data) -> {
                System.out.println("Got my processed data: " + data);    // Do some work in a callback
            });
        
        //...
    }
}
```

<!-- BENCHMARK -->
## Benchmark

Todo :)

<!-- CONTRIBUTING -->
## Contributing

Feel free to contribute to this project if you find something that is missing or can be optimized.
I want to retain the original vision of a simple yet usable library, so please keep that in mind when proposing new features.
If you do so, please follow the following steps:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request


<!-- CONTACT -->
## Contact

Maximilian Heidenreich - github@maximilian-heidenreich.de

Project Link: [https://github.com/MaximilianHeidenreich/JEventLoop](https://github.com/MaximilianHeidenreich/JEventLoop)

Project Icon: [https://github.com/MaximilianHeidenreich/JEventLoop/blob/master/assets/Icon-1024.png](https://github.com/MaximilianHeidenreich/JEventLoop/blob/master/assets/Icon-1024.png)

<a href="https://www.buymeacoffee.com/maximili"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=maximili&button_colour=5F7FFF&font_colour=ffffff&font_family=Cookie&outline_colour=000000&coffee_colour=FFDD00"></a>
