package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BoardService {
   public List<Board> getAllBoards(); //모든 게시판 조회
    public Board getBoardById(Long id);

    public void createBoard(Board board); //게시판 생성
    public void updateBoard(Board board); //게시판 수정
    public void deleteBoard(Long id); //게시판 삭제


}
