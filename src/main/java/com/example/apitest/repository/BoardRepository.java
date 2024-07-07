package com.example.apitest.repository;

import com.example.apitest.DTO.Board;

import java.util.List;

public interface BoardRepository {
    // 게시판 목록 조회
    public List<Board> findAll();

    // 게시판 상세 조회
    public Board findById(Long id);

    // 게시판 생성
    public void create(Board board);

    // 게시판 수정
    public void update(Board board);

    // 게시판 삭제
    public void delete(Long id);
}
