package com.todo.protocol;

public class CreateBoardPayload {
    private String boardName;

    public CreateBoardPayload() {}

    public CreateBoardPayload(String boardName) {
        this.boardName = boardName;
    }

    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }
}
