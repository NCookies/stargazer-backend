package xyz.ncookie.stargazer.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmailAndAuthProvider(String email, AuthProvider authProvider);

	Optional<Member> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

	boolean existsByEmail(String email);

	Optional<Member> findByEmail(String email);
}
