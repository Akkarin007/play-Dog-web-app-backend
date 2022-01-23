selectedState = []
var websocket;

var websocket = new WebSocket("ws://localhost:9000/websocket");

function initFieldListener() {
    document.querySelectorAll(".field").forEach((field) => {
        field.addEventListener('click', function () {
            selectAjax(field.id.replace("board", ""), this);
        })
    })
}

function initCardListeners() {
    document.querySelectorAll('button[name="card"]').forEach((field) => {
   
        field.addEventListener('click', function () {
            if(selectedState.length > 0){
                selectCardAndPiece(selectedState[0].piece, this);
            } else{
                selectCardAndPiece(0, this);
            }
        });
    });

    document.querySelectorAll('button[name="swapCard"]').forEach((field) => {
   
        field.addEventListener('click', function () {
            if (selectedState.length == 2){
                requestSwap(this);
            } else {
                alert('invalid move')
            }
        });
    });
}

function resetSelection(){
    selectedState.forEach((selectedEl) => {
        $(`#board${selectedEl.fieldIdx}`).css("background-color", "transparent");
    })
    selectedState = [];
}

function initDom() {
    initCardListeners();
    resetSelection();
}

function selectCardAndPiece( pieceNum, element) {
    var data =JSON.stringify({
        "type": "request",
        "cardNum": element.id,
        "cardOption": element.value,
        "pieceNum": parseInt(pieceNum)
    })
    websocket.send(data);
}

function requestSwap(element) {
    var data = JSON.stringify({
        "type": "swap",
        "cardNum": element.id,
        "otherPlayer": selectedState[1].playerIdx,
        "pieceNum1": selectedState[0].piece,
        "pieceNum2": selectedState[1].piece
    });
    websocket.send(data);

}

function selectAjax(fieldIdx, element) {
    $.ajax({
        method: "GET",
        url: '/isOwnPiece/' + fieldIdx,
        dataType: "json",
        
        success: function (result) {
            const array = result.isOwnPiece.split(" ");
            console.log("sas", array);
            selection(array, fieldIdx, element)
        }
    });
}

function selection(state, fieldIdx,  element){


    if (selectedState.length == 2) {
        resetSelection()
    } else if (!selectedState.find(function (selection) { return selection.fieldIdx === fieldIdx })) {
        
        if(state[0] == "true" && selectedState.length === 0 || state[0] == "false" && selectedState.length === 1) {
            const stateObj = getStateObj(state, fieldIdx);
            selectedState.push(stateObj);
            element.style.backgroundColor = 'goldenrod';
        } else {
            resetSelection();
            alert("invalid selection!")
        }
    }
}

function getStateObj(state, fieldIdx){
    return {
        fieldIdx: fieldIdx,
        selectedPiece: parseInt(state[1]),
        piece: parseInt(state[1]),
        playerIdx: parseInt(state[2]),
    }
}

function updatePlayerCardsPanel(cards) {
    let html = "";
    for (cardID in cards) {

        html += `<div class="card shadow-sm cards">
        <img src="/assets/images/cards/${cards[cardID]}.png" class="img-fluid shadow-lg " alt="Example image" loading="lazy" width="200">
        <div class="card-body">`;
        let innerHtml = "";
        const str_arr = cards[cardID].split(" ")
        for (entry in str_arr) {
            let cardName = str_arr[entry];
            if(cardName != "swapCard") {
                cardName = "card";
            }
            innerHtml += `<button class="btn btn-dark front" name="${cardName}" id="${cardID}" value="${entry}">${str_arr[entry]}</button>`
        }
        html +=innerHtml;
        html += `</div></div>`
    }
    
    $('.playCards').html(html);
}

function updatePlayerFigures(board) {
    for (player in board.players) {
        for (piece in board.players[parseInt(player)].piece_pos) {
            var piece_pos = board.players[parseInt(player)].piece_pos[parseInt(piece)]
            var field = document.querySelector("#board" + piece_pos)
            if (board.players[parseInt(player)].house.includes(parseInt(piece)) || board.players[parseInt(player)].garage.includes(parseInt(piece)) || field == null) {continue}
                field = field.querySelector("img")
                switch (board.players[parseInt(player)].color) {
                    case "red":
                        field.src = "/assets/images/icons/red.png";
                        break;
                    case "blau":
                        field.src = "/assets/images/icons/blau.png";
                        break;
                    case "yellow":
                        field.src = "/assets/images/icons/yellow.png";
                        break;
                    case "white":
                        field.src = "/assets/images/icons/white.png";
                        break;
                    case "green":
                        field.src = "/assets/images/icons/green.png";
                        break;
                    default:
                        break;
                }
        }
    }
}

function clearBoardFigures (size) {
    for (var x = 0; x < size; x++) {
        var field = document.querySelector("#board" + x)
        if (field == null) {return}
        field = field.querySelector("img")
        field.src = "/assets/images/icons/field.png"
    }

}

function updateGarage(board) {
    //TODO add after remodel of field
}

function updateStatsTable(board) {
    var html = ''
    
    for (player in board.players) {
        html += '<tr>'
        html += '<td>' + board.players[player].name + '</td>'
        html += '<td>' + board.players[player].house + '</td>'
        html += '<td>' + board.players[player].color + '</td>'
        html += '<td>' + board.players[player].garage + '</td>'
        html += '</tr>'
    }
    $('#stats_table').html(html)
}

function updateCurrenPlayerName(name) {
    if (document.querySelector("#player_name") == null) {
        return
    }
    document.querySelector("#player_name").textContent = "CurrentPlayerCards: " + name
}

function updateBoard(board) {
    resetSelection()
    clearBoardFigures(board.size);
    updatePlayerFigures(board);
    updateStatsTable(board);
    updateGarage(board);
    updateCurrenPlayerName(board.players[board.currentPlayer].name);
    updatePlayerCardsPanel(board.players[board.currentPlayer].cards);
    
}


class Board {
    constructor() {
        this.size = 0;
        this.numPlayers = 0;
        this.players = [];
        this.currentPlayer = 0;
    }

    fill(json) {
        this.size = json.boardSize
        this.numPlayers = json.playerNumber;
        this.currentPlayer = json.currentPlayer;
        this.players = [];
        for (var player in json.players) {
            var house = [];
            var pieces = [];
            var garage = [];
            var cards = [];

            for (var t_house in json.players[parseInt(player)].house) {
                house.push(json.players[parseInt(player)].house[parseInt(t_house)].inHouse);
            }
            for (var t_pieces in json.players[parseInt(player)].pieces) {
                pieces.push(json.players[parseInt(player)].pieces[parseInt(t_pieces)].piece_pos);
            }
            for (var t_garage in json.players[parseInt(player)].garage) {
                garage.push(json.players[parseInt(player)].garage[parseInt(t_garage)].garage_piece);
            }
            for (var t_cards in json.players[parseInt(player)].cards) {
                cards.push(json.players[parseInt(player)].cards[parseInt(t_cards)].card_symbol);
            }
            this.players.push(new Player(json.players[parseInt(player)].playerIdx,
                json.players[parseInt(player)].name,
                json.players[parseInt(player)].color,
                json.players[parseInt(player)].homePosition,
                pieces,
                garage,
                house,
                cards));
        }
        
    }
}

class Player {
    constructor(playerIdx, name, color, home, pieces, garage, house, cards) {
        this.name = name;
        this.playerIdx = playerIdx;
        this.color = color;
        this.home = home;
        this.piece_pos = pieces;
        this.garage = garage;
        this.house = house;
        this.cards = cards;
    }
}

function loadJsonAndUpdateDom(result) {
    board = new Board();
    board.fill(result);
    updateBoard(board);
    initDom();
}


function connectWebSocket() {
    websocket = new WebSocket("ws://localhost:9000/websocket");
    websocket.setTimeout
    websocket.onopen = function(event) {
        console.log("Connected to Websocket");
    }

    websocket.onclose = function () {
        console.log('Connection with Websocket Closed!');
    };

    websocket.onerror = function (error) {
        console.log('Error in Websocket Occured: ' + error);
    };

    websocket.onmessage = function (e) {
        if (typeof e.data === "string") {
            console.log("BoardChanged! - Websocket Push receiverd!")
            console.log(JSON.stringify(JSON.parse(e.data)))
            loadJsonAndUpdateDom(JSON.parse(e.data))
        }
    };
}


$(document).ready(function () {

    console.log("Document is ready, Filling Board!")

    $.ajax({
        method: "GET",
        url: "/json",
        dataType: "json",

        success: function (result) {
            loadJsonAndUpdateDom(result);
            initFieldListener();
        }
    });
    connectWebSocket();    
});

