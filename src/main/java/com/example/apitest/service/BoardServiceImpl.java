package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import com.example.apitest.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, JdbcTemplate jdbcTemplate) {
        this.boardRepository = boardRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    // 모든 게시판 조회
    @Override
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    @Override
    public Board getBoardById(Long id) {
        return boardRepository.findById(id);
    }

    // 게시판 생성
    @Override
    public void createBoard(Board board) {
        String sql = "INSERT INTO boards (title, content, writer, date) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getWriter(), LocalDateTime.now());
    }

    // 게시판 수정
    @Override
    public void updateBoard(Board board) {
        String sql = "UPDATE boards SET title = ?, content = ? WHERE id = ?";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getId());
    }

    // 게시판 글 삭제
    @Override
    public void deleteBoard(Long id) {
        String sql = "DELETE FROM boards WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }


}
