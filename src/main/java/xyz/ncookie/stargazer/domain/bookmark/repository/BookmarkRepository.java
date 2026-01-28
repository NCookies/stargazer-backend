package xyz.ncookie.stargazer.domain.bookmark.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	List<Bookmark> findAllByMember_Id(Long memberId);
}
