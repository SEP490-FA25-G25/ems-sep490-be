package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserBranches;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBranchesRepository extends JpaRepository<UserBranches, Long> {
}
