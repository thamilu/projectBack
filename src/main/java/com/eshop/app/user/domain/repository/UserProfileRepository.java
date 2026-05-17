package com.eshop.app.user.domain.repository;

import com.eshop.app.user.domain.entity.UserProfile;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
