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
    - [x] Queue events with callbacks that can be completed from inside event handlers.
- [x] Multithreaded

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
            <groupId>de.maximilian-heidenreich</groupId>
            <artifactId>jeventloop</artifactId>
            <version>2.3.0</version>
        </dependency>
    </dependencies>
    ....
</project>
```

#### Creating a custom Event

Custom events can be used to execute specific handlers for specific events. Custom events can store any data 
or logic you want them to. Just extends the default Event class and implement anything you need.

The `Event` class has a generic argument `D` which indicates the type of data that can be consumed inside callbacks.
If your Event does not need callbacks, feel free to use the `Void` type.

**⚠️ Make sure to call `super()` inside any custom constructor! Otherwise, your Event won't be handled!**

```java
class MyCoolEvent extends AbstractEvent<D> {        // "D" is the type of data your callbacks can consume.

    // You can store custom data associated with your Event
    public String message;

    // You can use a custom constructor to initialize your custom Event
    public MyCoolEvent(String message) {
        super();    // ! Call super() ! Otherwise the Event is corrupted and will be ignored !
        this.message = message;
    }

    // You can add custom logic to your Event
    public void print() {
        System.out.println(this.message);
    }

}
```

#### Setup an EventLoop

```java
class Main {
    public static void main(String[] args) {

      // Create a default Event loop (dispatcher: SingleThreadedExecutor, task executor: workStealingExecutor)
      EventLoop eventLoop = new EventLoop();
      
      // OR: Provide custom Executors
      EventLoop eventLoop = new EventLoop(dispatchExecutor, taskExecutor);
  
      // Start the Event loop (You probably want to register some handlers first tho!)
      eventLoop.start();

    }
}
```

#### Register Event handlers

You can register as many as you need, and they will be executed in order *(first registered -> first executed)*.

**⚠️ Make sure any code you call inside your handlers is thread safe!**

```java
class Main {
    public static void main(String[] args) {

        // ...

        // You can add a simple lambda function for small handlers
        eventLoop.addEventHandler(MyAwesomeEvent.class, (event) -> {
            event.print();
            // Do some other stuff
        });

        // You can add a function reference for bigger tasks
        eventLoop.addEventHandler(MyAwesomeEvent.class, Main::bigHandleLogic);

    }

    public static void bigHandleLogic(MyAwesomeEvent event) {

        // Do some work e.g. process data
        String data = event.message.toUpperCase();

        // If you want to break the chain of execution (stop executing handlers registered after this one):
        Event.cancel();

        // If there was an uncaught exception, it will get logged and succeeding handler
        // will still get executed!
        throw new Exception("Oops, something went wrong!");

        // Optionally complete callbacks when the data is ready
        event.complete(data);
        
        // Optionally except callbacks which gives you the opportunity to handle exceptions
        event.except(new Exception("You should really use a custom exception to know ehat went wrong!"));
        
    }
}
```

#### Queue an Event

If you enqueue an event, it will be handled some time in the future. If you depend on some returning data, 
you can use the returned `CompletableFuture<D>` to handle your data / any exceptions.

A possible usecase could be inside a networking application where you need to send a request and handle a response 
for it you will receive some time in the future.

**⚠️ Make sure any code you call inside your callbacks is thread safe!**

```java
class Main {
    public static void main(String[] args) {

        // Queue an event and ignore any callbacks
        eventLoop.dispatch(new MyAwesomeEvent("Hello World"));

        // Queue an event and handle callback data / exceptions
        eventLoop.dispatch(new MyAwesomeEvent("Hello Async World"))
                .thenApply(data -> {
                    System.out.println("Got my processed data: " + data);
                })
                .exceptionally(ex -> { ex.printStackTrace(); });

        //...
    }
}
```

<!-- BENCHMARK -->
## Benchmark

| Iterations | Average time / event | System                        | Description                                                                          |
|------------|----------------------|-------------------------------|--------------------------------------------------------------------------------------|
| 200 000    | < 0.5ms              | Mac Mini M1 (2020) - 16GB ram | No Thread.sleep()                                                                    |
|  20 000    |   1  ms              | Mac Mini M1 (2020) - 16GB ram | Random Thread.sleep(t) after each dispatch (0 < t < 6) & inside handler (0 < t < 4)  |
|            |                      |                               |                                                                                      |

**Note:**
These benchmarks do not reflect real application usage but try to simulate it somewhat using Thread.sleep() with random intervals.

- Time measured from creating the event instance till callback execution.
- The code was executed while my system was at ~50% CPU load.
- This is just a rough benchmark to test whether it can handle a good amount of basic throughput.
If you want to benchmark it yourself, feel free to do so and create a pull request afterwards to update the README.

**Benchmark code:**

```java
class Main {

    static Random ran = new Random();
    
    public static void main(String[] args) {

        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d %5p | %20C{1} | %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(Level.ERROR);
        Logger.getRootLogger().info("Starting ...");

        EventLoop eventLoop = new EventLoop();
        eventLoop.addEventHandler(TimingEvent.class, Test::testHandler);

        eventLoop.start();
        
        AtomicLong sum = new AtomicLong();
        int iterations = 200000;
        int i;
        AtomicInteger completed = new AtomicInteger();
        for (i = 0; i < iterations; i++) {
            System.out.println("Current iteration: " + i);
            TimingEvent e = new TimingEvent(System.currentTimeMillis());
            eventLoop.dispatch(e)
                    .thenAccept(s -> {
                        System.out.println("\t\t" + s + " ms");
                        sum.addAndGet(s.intValue());
                        completed.addAndGet(1);
                    });
            Thread.sleep(ran.nextInt(7));
        }
        while (completed != iterations) {}

        System.out.println("Average time: " + sum.longValue() / iterations  + "ms");
        
    }

    public static void testHandler(TimingEvent event) {
        try {
            int s = ran.nextInt(4);
            Thread.sleep(s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        event.complete(event.getCurrentTimeDiff());
    }
}

```

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
