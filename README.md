# SNS

- SNS 백엔드 프로젝트
- 단순 기능 구현 뿐만이 아닌 확장성, 장애 대처, 유지보수성을 고려하여 구현하는 것을 목표로 개발하였습니다.



### ✅ 사용 기술 및 개발 환경

Java, Spring Boot, MySQL, JPA, IntelliJ, Redis 등

### ✅ Architecture

- [MySQL Architecture](https://github.com/f-lab-edu/sns-itda/wiki/MySQL-Architecture)
- [Redis Architecture](https://github.com/f-lab-edu/sns-itda/wiki/Redis-Architecture)

![Architecture](https://user-images.githubusercontent.com/50859560/103137857-31791600-4710-11eb-9783-8a03dac3242c.png)



### ✅ ERD

자세한 사항은 👉 [https://github.com/f-lab-edu/sns-itda/wiki/ERD](https://github.com/f-lab-edu/sns-itda/wiki/ERD)

![](https://user-images.githubusercontent.com/50859560/103099160-b69cf600-4650-11eb-8bf6-505cbff6cf9a.png)



### ✅ 주요 기능

1. 게시글 CRUD
2. 댓글 CRUD
3. 게시글 좋아요
4. 피드 생성


### ✅ 주요 고려 사항

* #### Mysql master - slave
대규모 서비스에서 단일 데이터베이스의 한계를 극복하기 위한 방법으로, 쓰기 작업은 마스터 DB가 담당하고 읽기 작업은 여러 슬레이브 DB로 분산하는 구조를
구성했습니다. 또한 라운드로빈 방식으로 로드밸런싱하여 읽기 요청 부하를 분산하였습니다.

JPA와 DB를 연동할 때 master-slave 요청을 구분하기 위해 두 가지 방식을 고민했습니다. 
일반적으로 사용되는 Transactional(readonly=true/false) 방식
은 구현이 단순하고 코드 중복이 없으며, 적은 수의 빈으로 메모리가 효율적이고 JPA 영속성 컨텍스트 최적화가 자동으로 적용되는 장점이 있지만, 트랜잭션 속성
누락 시 의도치 않은 Master DB 접근이 가능하고 코드만으로는 어떤 DB를 사용하는지 파악이 어려우며 런타임에서야 DB 접근 오류를 발견할 수 있다는 단점이
있습니다. 반면 Repository 물리적 분리 방식은 컴파일 타임에 잘못된 DB 접근을 방지하고 코드만으로도 DB 접근 의도 파악이 명확하며 각 DB에 최적화된 설정
을 적용할 수 있지만, Repository 코드 중복이 발생하고 더 많은 설정 코드가 필요하며 빈 개수 증가로 인한 메모리 사용량 증가와 패키지 구조가 복잡해진다는 단
점이 있습니다.
저는 Transactional(readonly=true/false) 방식으로 요청을 구분하기로 결정했습니다. 코드 중복이 없어 유지보수가 쉽고 휴먼 에러 가능성이 작으며, JPA의
읽기 전용 최적화(스냅샷 미생성, 더티 체킹 스킵)를 자연스럽게 활용할 수 있고, AOP를 통한 트랜잭션 처리로 비즈니스 로직과 인프라 로직이 깔끔하게 분리됩니
다. Repository 분리 방식은 명시성이라는 장점이 있지만, 실제로 트랜잭션 속성 누락으로 인한 문제는 테스트 단계에서 충분히 발견할 수 있고, CI/CD 파이프라
인에서의 테스트 자동화로 런타임 오류를 사전에 방지할 수 있으며, 모니터링과 로깅을 통해 DB 접근 패턴을 추적할 수 있습니다. 결과적으로 Repository 분리는
실제 문제를 해결하기 위한 비용이 이점보다 크고, 패키지 구조 복잡화와 빈 증가로 인한 개발 생산성 저하가 발생하므로, "과도한 엔지니어링은 좋은 엔지니어링
이 아니다"라는 관점에서 Transactional(readonly=true/false) 방식이 더 현명한 선택이라고 생각했습니다.
* AOP를 적용하여 부가 로직 제거
* 프로퍼티 파일을 이용한 외부 설정 주입과 운영 환경에 따른 프로퍼티 파일 분리
* Spring Cache 적용으로 읽기 작업 성능 향상
* Redis LFU Eviction 정책을 적용하여 효율적인 캐시 띄우기
* Redis 성능 향상을 위한 Redis 세션 저장소와 캐시 저장소의 분리
* 부하 분산을 위한 MySQL Replication 구성 및 쿼리 요청 분기