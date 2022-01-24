package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import dog._
import dog.controller.ControllerComponent.ControllerTrait
import dog.controller.ControllerComponent.controllerBaseImpl.Controller
import dog.model.BoardComponent.boardAdvancedImpl.Board
import dog.controller.StateComponent.InputCardMaster
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import dog.controller.BoardChanged
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.{JsNumber, JsString, JsObject, Json, JsValue, JsResultException}
import play.api.libs.streams.ActorFlow
import scala.swing.Reactor
import scala.swing.event.Event
import scala.swing.Publisher
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)(implicit system: ActorSystem, mat: Materializer) extends BaseController {
  
  var gameLobbies = Map("Default" -> new Lobby())
  
  def addLobby(lobbyID: String): Unit = {
    gameLobbies += (lobbyID -> new Lobby());
  }

  def removeLobby(lobbyID: String): Unit = {
    gameLobbies -= lobbyID;
  }

  class Lobby() {
    var controller = new Controller(new Board(20))
    var running = false
    var lobbySize = 4
    var playerNames = List[String]()

    def addPlayer(playername: String): Unit = {
      if (!playerNames.contains(playername) && playerNames.size < lobbySize) {
        playerNames = playerNames :+ playername
      }
    }

    def removePlayer(playername:String): Unit = {
      if (playerNames.contains(playername) && playerNames.size <= lobbySize) {
        var new_list = playerNames.filter(_ != playername)
        playerNames = new_list
      }
    }
  }

  var playerNames = List("Player1","Player2","Player3")
  addLobby("Default")
  gameLobbies("Default").controller.initGame(playerNames, 4, 6, 20)
  gameLobbies("Default").running = true
  gameLobbies("Default").playerNames = playerNames
  
  def printDog() = gameLobbies("Default").controller.toStringBoard + "/n" + gameLobbies("Default").controller.toStringGarage + "/n" + gameLobbies("Default").controller.toStringPlayerHands + "/n" + gameLobbies("Default").controller.lastMessage

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def about: Action[AnyContent] = Action {
    Ok(views.html.about())
  }

  def initGame: Action[AnyContent] = Action {
    Ok(views.html.initGame(gameLobbies("Default").controller))
  }

  def newGame(amountPieces: Int, amountCards: Int, sizeBoard: Int) = Action {
    var playerNames = List("P1","P2","P3","P4")
    gameLobbies("Default").controller.initGame(playerNames, amountPieces, amountCards, sizeBoard)
    gameLobbies("Default").playerNames = playerNames
    Ok(views.html.initGame(gameLobbies("Default").controller))
  }
  
  def isOwnPiece(fieldIdx: String) = Action {
    var result = false;
    var jsonResultString: String = "";
    gameLobbies("Default").controller.gameState.board.cell(fieldIdx.toInt).p match { 
      case Some(p)=> { 
          result = (p.nameAndIdx._1 == gameLobbies("Default").controller.gameState.actualPlayer.nameAndIdx._1)

          var pieceIdx = p.getPieceNum(fieldIdx.toInt);
          if (result) {
            jsonResultString = "true " + pieceIdx + " " + p.nameAndIdx._2
          } else {
            jsonResultString = "false " + pieceIdx + " " + p.nameAndIdx._2
          }
      }
      case None => {
          jsonResultString = "none"
      }
   
  }
     Ok(Json.obj(
        "isOwnPiece" -> jsonResultString
      ))
}
  def selectCardWithOption(cardNum: Int, cardOption: Int) = Action {
    gameLobbies("Default").controller.manageRound(InputCardMaster.UpdateCardInput()
              .withCardNum((cardNum, cardOption))
              .withSelectedCard(gameLobbies("Default").controller.actualPlayedCard(cardNum))
              .buildCardInput())
    Ok(views.html.initGame(gameLobbies("Default").controller))
    }

  def selectSwap: Action[JsValue] = Action(parse.json) {
    setRequest: Request[JsValue] => {
      val cardNum = (setRequest.body \ "cardNum").as[String]
      val otherPlayer = (setRequest.body \ "otherPlayer").as[Int]
      val pieceNum1 = (setRequest.body \ "pieceNum1").as[Int]
      val pieceNum2 = (setRequest.body \ "pieceNum2").as[Int]
      swap(cardNum, otherPlayer, pieceNum1, pieceNum2)
      Ok(boardToJson("Default"))
    }
  }

  def swap(cardNum: String, otherPlayer: Int, pieceNum1: Int, pieceNum2: Int): Unit = {
    val fieldPosOwn = gameLobbies("Default").controller.gameState.actualPlayer.piece(pieceNum1).pos
      val fieldPosOther = gameLobbies("Default").controller.gameState.players._1(otherPlayer).piece(pieceNum2).pos
              gameLobbies("Default").controller.selectedField(fieldPosOwn)
              gameLobbies("Default").controller.selectedField(fieldPosOther)
              gameLobbies("Default").controller.manageRound(InputCardMaster.UpdateCardInput()
                .withOtherPlayer(otherPlayer)
                .withCardNum((cardNum.toInt, 0))
                .withSelectedCard(gameLobbies("Default").controller.actualPlayedCard(cardNum.toInt))
                .buildCardInput())
  }
    
  def selectCard(cardNum: Int) = Action {
    gameLobbies("Default").controller.manageRound(InputCardMaster.UpdateCardInput()
              .withCardNum((cardNum, 2))
              .withSelectedCard(gameLobbies("Default").controller.actualPlayedCard(cardNum))
              .buildCardInput())
    Ok(views.html.initGame(gameLobbies("Default").controller))
    }
  
  def selectCardAndPiece: Action[JsValue] = Action(parse.json) {
    setRequest: Request[JsValue] => {
      val cardNum = (setRequest.body \ "cardNum").as[String]
      val cardOption = (setRequest.body \ "cardOption").as[String]
      val pieceNum = (setRequest.body \ "pieceNum").as[Int]
      val fieldPos = gameLobbies("Default").controller.gameState.actualPlayer.piece(pieceNum.toInt).pos
      gameLobbies("Default").controller.selectedField(fieldPos)
      gameLobbies("Default").controller.manageRound(InputCardMaster.UpdateCardInput()
        .withCardNum((cardNum.toInt, cardOption.toInt))
        .withSelectedCard(gameLobbies("Default").controller.actualPlayedCard(cardNum.toInt))
        .buildCardInput())
      Ok(boardToJson("Default"))
    }
  }

  def printBoard() = Action {
    Ok(printDog())
  }

  def load() = Action {
    gameLobbies("Default").controller.load
    Ok("load /n" + printDog())
  }

  def save() = Action {
    gameLobbies("Default").controller.save()
    Ok("load /n" + printDog())
  }

  def undo() = Action {
    gameLobbies("Default").controller.undoCommand()
    Ok(views.html.initGame(gameLobbies("Default").controller))
  }

  def redo() = Action {
    gameLobbies("Default").controller.redoCommand()
    Ok(views.html.initGame(gameLobbies("Default").controller))
  }

  def playerhands() = Action {
    Ok("playerhands" + gameLobbies("Default").controller.toStringPlayerHands)
  }

  def createPlayers(name1: String, name2: String, name3: String, name4: String, amountPieces: Int, amountCards: Int) = Action {
    Ok("createNewBoard" + gameLobbies("Default").controller.createPlayers(List(name1,name2,name3,name4), amountPieces, amountCards))
  }


  def getJsonBoard: Action[AnyContent] = Action {
    Ok(boardToJson("Default"))
  }

  def boardToJson(lobbyID: String): JsObject = {
    val controller = gameLobbies(lobbyID).controller
    return Json.obj(
      // board data
      "lobbyID" -> lobbyID,
      "boardSize" -> JsNumber(controller.gameState.board.size),
      // player data
      "playerNumber" -> JsNumber(controller.gameState.players._1.size),
      "currentPlayer" -> JsNumber(controller.gameState.actualPlayer.nameAndIdx._2),
      "players" -> Json.toJson(
        for {
          idx <- 0 until controller.gameState.players._1.size,
        } yield {
          Json.obj(
            "playerIdx" -> JsNumber(idx),
            "name" -> controller.gameState.players._1(idx).nameAndIdx._1,
            "color" -> controller.gameState.players._1(idx).color,
            "homePosition" -> JsNumber(controller.gameState.players._1(idx).homePosition),
            "pieces" -> Json.toJson(
              for {
                piece_idx <- 0 until controller.gameState.players._1(idx).piece.size,
              } yield {
                Json.obj(
                  "piece_idx" -> JsNumber(piece_idx),
                  "piece_pos" -> JsNumber(controller.gameState.players._1(idx).piecePosition(piece_idx))
                )
              }
            ),
            "garage" -> Json.toJson(
              for {
                garage_idx <- 0 until controller.gameState.players._1(idx).garage.size
              } yield {
                Json.obj(
                  "garage_idx" -> JsNumber(garage_idx),
                  "garage_piece" -> JsNumber(controller.gameState.players._1(idx).garage.getPieceIndex(garage_idx))
                )
              }
            ),
            "house" -> Json.toJson(
              for {
                house_idx <- 0 until controller.gameState.players._1(idx).inHouse.length
              } yield {
                Json.obj(
                  "inHouse" -> JsNumber(controller.gameState.players._1(idx).inHouse(house_idx))
                )
              }
            ),
            "cards" -> Json.toJson(
              for {
                card_idx <- 0 until controller.gameState.players._1(idx).cardList.length
              } yield {
                Json.obj(
                  "card_symbol" -> controller.gameState.players._1(idx).cardList(card_idx).symbol
                )
              }
            )
          )
        }
      )
    )
  }

  def lobbyListToJson(): JsObject = {
    Json.obj(
      "lobbies" -> Json.toJson(
        for {
          (key,value) <- gameLobbies
        } yield {
          Json.obj(
            "lobbyID" -> key,
            "lobbyInGame" -> value.running,
            "lobbySize" -> value.lobbySize,
            "lobbyPlayers" -> Json.toJson(
              for {
                name <- value.playerNames
              } yield {
                Json.obj(
                  "playerName" -> name
                )
              }
            )
          )
        }
      )
    )
  }

  def socket: WebSocket = WebSocket.accept[String, String] { _ =>
    ActorFlow.actorRef { out =>
      println("Connection received.")
      DogWebSocketActorFactory.create(out)
    }
  }

  object DogWebSocketActorFactory {
    def create(out: ActorRef): Props = {
      Props(new DogWebSocketActor(out))
    }
  }
  
  class LobbyEventPublisher extends Publisher{
    def newEvent(event: Event): Unit = {
      publish(event)
    }
  }
  val eventPublisher = new LobbyEventPublisher 

  class RemovedLobby(id: String) extends Event {
    def id(): String = {id}
  }

  class LobbyUpdate extends Event

  class DogWebSocketActor(out: ActorRef) extends Actor with Reactor {
    listenTo(eventPublisher)
    listenTo(gameLobbies("Default").controller)
    var lobbyID: String = "Default"
    def receive: Actor.Receive = {
      case msg: String =>
        val msgObject: JsValue = Json.parse(msg)
        val msgType = (msgObject \ "type").as[String]
        lobbyID = "Default"
        try {
          lobbyID = (msgObject \ "lobbyID").as[String]
        } catch {
          case e: JsResultException => 
        }
        if(msgType == "swap" && checkLobbyID(lobbyID)) {
         
          val cardNum = (msgObject \ "cardNum").as[String]
          val otherPlayer = (msgObject \ "otherPlayer").as[Int]
          val pieceNum1 = (msgObject \ "pieceNum1").as[Int]
          val pieceNum2 = (msgObject \ "pieceNum2").as[Int]
          val controller = gameLobbies(lobbyID).controller
          val fieldPosOwn = controller.gameState.actualPlayer.piece(pieceNum1).pos
          val fieldPosOther = controller.gameState.players._1(otherPlayer).piece(pieceNum2).pos
                  controller.selectedField(fieldPosOwn)
                  controller.selectedField(fieldPosOther)
                  controller.manageRound(InputCardMaster.UpdateCardInput()
                    .withOtherPlayer(otherPlayer)
                    .withCardNum((cardNum.toInt, 0))
                    .withSelectedCard(controller.actualPlayedCard(cardNum.toInt))
                    .buildCardInput())
          out ! boardToJson(lobbyID).toString()
        }
        if(msgType == "request" && checkLobbyID(lobbyID)) {
          val cardNum = (msgObject \ "cardNum").as[String]
          val cardOption = (msgObject \ "cardOption").as[String]
          val pieceNum = (msgObject \ "pieceNum").as[Int]
          val controller = gameLobbies(lobbyID).controller
          val fieldPos = controller.gameState.actualPlayer.piece(pieceNum.toInt).pos
          controller.selectedField(fieldPos)
          controller.manageRound(InputCardMaster.UpdateCardInput()
            .withCardNum((cardNum.toInt, cardOption.toInt))
            .withSelectedCard(controller.actualPlayedCard(cardNum.toInt))
            .buildCardInput())
          out ! boardToJson(lobbyID).toString()
        }
        if(msgType == "startGame" && checkLobbyID(lobbyID)) {
 
          val cardNum = (msgObject \ "cardNum").as[Int]
          val pieceNum = (msgObject \ "pieceNum").as[Int]
          val size = (msgObject \ "size").as[Int]
          gameLobbies(lobbyID).controller.initGame(gameLobbies(lobbyID).playerNames, pieceNum, cardNum, size)
          gameLobbies(lobbyID).running = true
          println("Received Json: Start Game")
          out ! lobbyListToJson().toString()
          out ! boardToJson(lobbyID).toString()
        }
        if(msgType == "getLobbies") {
          out ! lobbyListToJson().toString()
        }
        if(msgType == "createLobby") {
          val name = (msgObject \ "playerName").as[String]
          val size = (msgObject \ "lobbySize").as[Int]
          addLobby(lobbyID)
          gameLobbies(lobbyID).addPlayer(name)
          gameLobbies(lobbyID).lobbySize = size
          listenTo(gameLobbies(lobbyID).controller)
          eventPublisher.newEvent(new LobbyUpdate)
        }
        if(msgType == "joinLobby" && checkLobbyID(lobbyID)) {
       
          val name = (msgObject \ "playerName").as[String]
          gameLobbies(lobbyID).addPlayer(name)
          listenTo(gameLobbies(lobbyID).controller)
          eventPublisher.newEvent(new LobbyUpdate)
        }
        if(msgType == "leaveLobby" && checkLobbyID(lobbyID)) {

          val name = (msgObject \ "playerName").as[String]
          gameLobbies(lobbyID).removePlayer(name)
          if (gameLobbies(lobbyID).playerNames.length == 0) {
            removeLobby(lobbyID)
            eventPublisher.newEvent(new RemovedLobby(lobbyID))
          } else {
            eventPublisher.newEvent(new LobbyUpdate)
          }
        }
        if(msgType == "endGame" && checkLobbyID(lobbyID)) {

          removeLobby(lobbyID)
          eventPublisher.newEvent(new RemovedLobby(lobbyID))
        }
        
        if(msgType == "getBoard" && checkLobbyID(lobbyID)) {

          out ! boardToJson(lobbyID).toString()
        }
        println("Received Json: " + msg + "trigger Event")
    }

    reactions += {
      case event: BoardChanged => sendJsonToClient()
      case event: LobbyUpdate => sendLobbiesToClient()
      case event: RemovedLobby => sendDeletedLobbiesToClient(event.id)
    }

    def sendJsonToClient(): Unit = {  
      if (!checkLobbyID(lobbyID)) {
            return
      }
      println("Received event from Controller")
      out ! boardToJson(lobbyID).toString()
    }

    def sendLobbiesToClient(): Unit = {
      println("Received event from Controller")
      out ! lobbyListToJson().toString()
    }

    def sendDeletedLobbiesToClient(id: String): Unit = {
      println("Received event from Controller")
      val response = Json.obj(
        "removedLobbyID" -> id
      )
      out ! response.toString()
    }
    def checkLobbyID(lobbyID: String): Boolean = {
      gameLobbies.contains(lobbyID)
    }

  }
}

