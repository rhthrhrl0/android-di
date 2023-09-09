# android-di
## 2단계
기능 요구 사항
필드 주입
- [x] 어노테이션을 필요한 요소에만 붙여서, 필드 주입을 구현한다.
- [x] 내가 만든 의존성 라이브러리가 제대로 작동하는지 테스트 코드를 작성한다.

재귀 DI
- [x] CartRepository가 DAO 객체를 참조하도록 변경한다.
- [x] CartProductViewHolder의 bind 함수를 수정해서, 장바구니에 상품이 담긴 날짜 정보를 확인 가능하도록 한다.

선택 요구 사항
- [] 장바구니에서 항목을 클릭해서 삭제할 때, 리사이클러뷰의 포지션이 아닌, id를 기준으로 삭제한다.
- [] 뷰에서 CartProductEntity를 직접 참조하지 않는다.