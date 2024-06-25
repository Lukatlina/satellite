<?php
  // DB 연결 정보
  $serverName = "127.0.0.1";
  $userName = "root";
  $server_pw = "spwmal1210";
  $dbName = "satellite";

  // DB 연결
    
  $conn = mysqli_connect( $serverName , $userName , $server_pw , "satellite" );
    
  $sql = "SELECT * FROM user WHERE user_id=1;";
    
  $result = mysqli_query( $conn, $sql );
    
  // 유저 고유번호와 이메일을 함께 조회해서 가져온다.

  $row = mysqli_fetch_assoc($result);
    
  echo $row[ 'user_id' ], $row[ 'email' ];

?>