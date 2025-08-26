package capstone.ballkeeper.repository;

import capstone.ballkeeper.domain.member.Member;
import capstone.ballkeeper.domain.member.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);

    List<Member> findByRole(Role role);
}