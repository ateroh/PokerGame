# Project 3 - Online - Poker

> Replace 3 with your group number. Replace Online - Poker with your project's name.

# Abstract

> This project implements a distributed multiplayer poker game using tuple spaces for coordination and JavaFX for the graphical interface. The system follows a model view controller paired with a client-server architecture where a central server manages game state through tuple space repositories, while multiple clients connect remotely to participate. 

The server handles player registration, card dealing, and game flow management through coordinated tuple operations. Communication occurs using blocking and non-blocking tuple space operations for turn management . The client uses the MVC pattern, automatically updating
the JavaFX interface. Players can join games, place bets (fold, call, raise), and compete in Texas Hold'em poker using standard rules and hand evaluation.

# Contributors

> Replace your names and email addressses below

Project contributors:
* Limejensen (s240617@student.dtu.dk)
* BrinchyBoy (s246072@student.dtu.dk)
* ateroh     (s245803@student.dtu.dk)


> Indicate the name of main people contributing to each part of the project below (keep the bullet points!). Note that the report as a whole is under the joint responsibility of the entire
group. 

Contributions:
* Design of main coordination aspects: Alice, Bob.
* Coding of main coordination aspects: Bob, Charlie.
* Documentation (this README file): Charlie, Dave.
* Videos: Dave, Alice.
* Other aspects (e.g. coding of UI, etc.): Alice, Dave.

> IMPORTANT: The history of the repository must show that *all* members have been active contributors.

# Demo video

> Add here a link to a video showing how your project runs. The video does not need to explain anything about how you designed or coded your project. Just show how it runs. There is no time limit or specific format. You can put it on YouTube or similar.

Demo video: https://youtu.be/paWE-GvDO1c?si=SR6srFgJOtMZ1ECE

> If your demo video uses one single computer (as it would be easier to screencast), please add a link to an additional video showing that it can also run on multiple computers.

Running on multiple computers video: https://www.youtube.com/shorts/76W1ZtZfgFk

> As a back-up, please upload the videos as part of your submission.

# Main coordination challenge

> Most likely you have been addressing and sovling many coordination challenges. Which one was the most challenging? Which solution are you most proud?. Choose just one. Use a few diagrams to illustrate the challenge and the solution. Use a few paragraphs to explain it. Refer to the materials from the mandatory materials (modules 1-3) and recommended materials (modules 4-6, Klempmann's course, etc.). That is, use the precise terminology and add references. This part shold not be longer than 2 screens (approx.).

We encountered several coordination challenges while building the distributed PokerGame, as for example our lobbymanagement where we ensured atomic registration of players using a global lock pattern (lecture 2). This ensures “mutual exclusion” when multiple clients try to join simultaneously.

We also have our private game rooms where we used private space pattern (lecture 3). The server acts as a space repository that creates new game spaces and distributes their URIs via a remotespace.

And lastly our Deckmanagement where we implemented randomspace (lecture 1) for random retrieval which our project relies largely on for the non deterministic nature of tuple retrieval, when multiple matching tuples exists in the form of game cards.

The most challenging was however our distributed sequential turn management. The challenge was ensuring sequential consistency in our environment. In poker the game flows allows only one specific player to act at a time based on strict rules and order (Small blind, big blind and so on) effectively implementing a strict protocol (Lecture 4). The solution was ordered coordination via blocking operations.

The GameModel (server) and players (clients) operates in a producer-consumer relationship (lecture 2), passing a “turn” token.

When the token has been passed the server explicitly targets the next player. It puts a directed tuple containing the specific player names:

```java
gameSpace.put("yourTurn", player.getName(), currentBet, 
player.getChips(), lastRaiseAmount);
```
All clients posses a listener thread, but they utilize pattern matching (lecture 1) to filter the stream. A client uses a blocking operation “get” (lecture 2) to wait until a tuple matches its specific local name. This effectively pauses the clients execution until it is their turn.

```java
Object[] t = gamessapace.get(
   new ActualField("yourTurn"), new ActualField(getMyName()), // Pattern 
   new FormalField(Integer.class), ...
```
Once the user acts (fold/call/raise), the client produces an “action” tuple back. The server, acting as a consumer (lecture 2) for this specific response has been waiting (blocking). It consumes this tuple, updates the global state and then proceeds to produce the turn for the next player.

```java
Object[] action = gameSpace.get(new ActualField("action"), new ActualField(playerName), ...);
```

# Programming language and coordination mechanism

> If you use a tuple space library from [pSpaces](https://github.com/pSpaces) just write something like "This project is based on the tuple space library X" where X is jSpace, dotSpace, etc.
> If you use something else implement coordination, you will have to describe here how the coordination concepts and mechanisms in the frameworks you use relate to the ones we use in the course. For example, if you decide to use Erlang you will have to explain inboxes and all that in terms of tuple spaces. The goal should be that anyone that has followed the couse materials, should be able to understand what you did, even if you decided not to use pSpaces libraries.

# Installation

> Provide here installation requirements and instructions. Your goal here is that anyone else following the course should be able to follow the steps and run your project.

# References 
> List here the main references that you have been using in your text above (course materials, articles, webtsites, etc.)

# IMPORTANT!

> Feel free to remove all comments of the template, but please **keep the section structure** (Project, Abstract, ...)