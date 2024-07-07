package com.example.apitest.repository;

import com.example.apitest.DTO.Board;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcBoardRepository implements BoardRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcBoardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Board> findAll() {
        String sql = "SELECT * FROM boards";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Board.class));
    }

    @Override
    public Board findById(Long id) {
        String sql = "SELECT * FROM boards WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Board.class));
    }

    @Override
    public void create(Board board) {
        String sql = "INSERT INTO boards (title, content) VALUES (?, ?)";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent());
    }

    @Override
    public void update(Board board) {
        String sql = "UPDATE boards SET title = ?, content = ? WHERE id = ?";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getId());
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM boards WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}