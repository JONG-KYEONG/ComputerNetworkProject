// 'use strict';


var chatRoomPage = document.querySelector('#chatroom-page');
var chatPage = document.querySelector('#chat-page');
var exitButton = document.querySelector('#exit-chat-room');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');
var startButton = document.getElementById('startButton');

var stompClient = null;
var username = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function setPlaceholderOnLoad() {
    // const savedUsername = localStorage.getItem("username");
    const nameInput = document.querySelector('#name'); // <input> 요소 선택

    // localStorage에 저장된 값 확인 및 기본값 설정
    if (localStorage.getItem("username") === null || localStorage.getItem("username") === "") {
        localStorage.setItem("username", "기본 닉네임");
    }

    // placeholder에 localStorage 값 설정
    nameInput.placeholder = localStorage.getItem("username");
}

// 페이지 로드 시 실행
document.addEventListener("DOMContentLoaded", setPlaceholderOnLoad, true);


async function enterGame(event) {
    // chatRoomPage.classList.add('hidden');
    // usernamePage.classList.remove('hidden');

    var url = "http://localhost:8080/api/game";
    await fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text();
        })
        .then(data => {
            localStorage.setItem('gameId', data);
        });


    connect();
}

async function createGame(event) {
    var url = "http://localhost:8080/api/create/game"
    await fetch(url, {
        method: 'POST',
        headers: {
            'Content-type': 'application/json'
        }
    })
        .then(response => {
            return response.json();
        })
        .then(data => {
            if (data >= 0) {
                localStorage.setItem('gameId', data);
            }
        });
    event.preventDefault(); //페이지가 다시 로드 되는 것을 막는다.

    connect();
}

function connect(event) {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnected, onError);

    chatRoomPage.classList.add('hidden');
    chatPage.classList.remove('hidden');
    localStorage.setItem('gaming', false);

    event.preventDefault();
}


function onConnected() {
    //localstorage에서 아이템 꺼내기
    var gameId = localStorage.getItem('gameId');
    var username = localStorage.getItem('username');


    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public/' + gameId, onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({sender: username, messageType: 'JOIN', gameId: gameId})
    )


    //GET으로 roomName들고 오기
}

function saveName(event) {
    event.preventDefault()
    username = document.querySelector('#name').value.trim();
    localStorage.setItem("username", username);
    const nameInput = document.querySelector('#name'); // <input> 요소 선택
    nameInput.placeholder = localStorage.getItem("username");
}

function exitChatRoom(event) {
    stompClient.unsubscribe();
    localStorage.removeItem('roomId');

    chatPage.classList.add('hidden');
    chatRoomPage.classList.remove('hidden');

    location.reload();
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    var gameId = localStorage.getItem('gameId');

    if (messageContent && stompClient) {
        var chatMessage = {
            sender: localStorage.getItem('username'),
            senderId: localStorage.getItem('userId'),
            content: messageInput.value,
            messageType: 'CHAT',
            gameId: gameId
        };

        if(localStorage.getItem('gaming')==="true"){
            stompClient.send("/app/chat.correctAnswer", {}, JSON.stringify(chatMessage));
            messageInput.value = '';
        }
        else {
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            messageInput.value = '';
        }
    }
    event.preventDefault();
}

function startGame() {
    startButton.classList.add('hidden');

    stompClient.send("/app/chat.startGame",
        {},
        JSON.stringify({
            sender: localStorage.getItem('username'),
            senderId: localStorage.getItem('userId'),
            content: messageInput.value,
            messageType: 'START',
            gameId: localStorage.getItem('gameId')
        })
    )

}

function addImageToChatHeader(imageUrl) {
    var chatHeader = document.querySelector('.image');

    // 기존에 이미지가 있는지 확인
    var existingImage = chatHeader.querySelector('img');
    if (existingImage) {
        // 기존 이미지가 있으면 삭제
        existingImage.remove();
    }


    // 새로운 이미지 요소 생성
    var img = document.createElement('img');

    // 이미지 URL 설정
    img.src = imageUrl;

    // 이미지 크기 조정 (필요한 경우)
    img.style.width = 'auto';  // 너비 설정 (필요에 맞게 조정)
    img.style.height = '350px'; // 높이 설정 (필요에 맞게 조정)
    img.style.margin = "10px auto"; // 중앙 정렬 및 간격 추가
    img.style.display = 'block';  // 이미지가 블록 요소처럼 동작하도록 설정 (줄바꿈)
    img.style.marginBottom = '10px'; // 이미지 아래 마진 추가 (줄바꿈 효과)

    chatHeader.appendChild(img);
}

function removeImageFromChatHeader() {
    var chatHeader = document.querySelector('.image');

    var image = chatHeader.querySelector('img');
    if (image) {
        image.remove();  // 이미지 삭제
    }
}

function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    var messageElement = document.createElement('li');

    if (message.messageType === 'JOIN') {
        messageElement.classList.add('event-message');

        message.gameUserDtos.forEach(gameUserDto => {
            if (gameUserDto.gameNickname === localStorage.getItem('username')) {
                localStorage.setItem("userId", gameUserDto.userId); // 바로 gameUserDto를 사용
                if (gameUserDto.isCaptain) {
                    startButton.classList.remove('hidden');
                } else {
                    startButton.classList.add('hidden');
                }
            }
        });

    } else if (message.messageType === 'LEAVE') {
        messageElement.classList.add('event-message');

        message.gameUserDtos.forEach(gameUserDto => {
            if (gameUserDto.gameNickname === localStorage.getItem('username')) {
                if (gameUserDto.isCaptain) {
                    startButton.classList.remove('hidden');
                } else {
                    startButton.classList.add('hidden');
                }
            }
        });

    } else if(message.messageType === 'START'){
        message.sender = "관리자 ";
        addImageToChatHeader(message.gameInfoDto.gameAnswerImage);
        localStorage.setItem('gaming', true);
    } else if(message.messageType === 'END'){
        message.sender = "관리자 ";
        removeImageFromChatHeader();
        message.gameUserDtos.forEach(gameUserDto => {
            if (gameUserDto.gameNickname === localStorage.getItem('username')) {
                if (gameUserDto.isCaptain) {
                    startButton.classList.remove('hidden');
                } else {
                    startButton.classList.add('hidden');
                }
            }
        });
        localStorage.setItem('gaming', false);
    }
    else if(message.messageType === 'ANSWER'){
        message.sender = "관리자 ";
        addImageToChatHeader(message.gameInfoDto.gameAnswerImage);
    }
    else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var avatarText = document.createTextNode("-");
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }


    var messageText = document.createTextNode(message.content);

// 줄바꿈 문자를 <br>로 변환
    var formattedMessage = message.content.replace(/\n/g, '<br>');

// 줄바꿈이 반영된 HTML을 삽입
    var textElement = document.createElement('p');
    textElement.innerHTML = formattedMessage; // innerHTML을 사용하여 <br> 태그 적용

    messageElement.appendChild(textElement);
    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}

messageForm.addEventListener('submit', sendMessage, true)
exitButton.addEventListener('click', exitChatRoom, true);


const enterGameButton = document.querySelector('#enter-game');
const createGameButton = document.querySelector('#create-game');

usernameForm.addEventListener('submit', saveName, true)
enterGameButton.addEventListener('click', enterGame, true);
createGameButton.addEventListener('click', createGame, true);


startButton.addEventListener('click', startGame, true)
