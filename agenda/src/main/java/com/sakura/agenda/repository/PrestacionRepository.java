package com.sakura.agenda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sakura.agenda.model.Prestacion;

@Repository
public interface PrestacionRepository extends JpaRepository<Prestacion, Long> {

	boolean existsByProfesionalId(Long profesionalId);

}
