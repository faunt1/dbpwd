#!/bin/bash

echo "
==========================================
继续测试所有剩余Demo
==========================================
" >> demo-test-results.txt

# spring-boot
echo "==========================================" >> demo-test-results.txt
echo "TEST: demos/spring-boot/demo" >> demo-test-results.txt
echo "==========================================" >> demo-test-results.txt
java -Dfile.encoding=UTF-8 -jar demos/spring-boot/demo/target/spring-boot-demo-1.0.0.jar 2>&1 | grep -E "(Started|users)" >> demo-test-results.txt &
SB_PID=$!
sleep 8
kill $SB_PID 2>/dev/null
echo "" >> demo-test-results.txt
echo "--- END: spring-boot/demo ---" >> demo-test-results.txt
echo "" >> demo-test-results.txt

# spring-boot/original
echo "==========================================" >> demo-test-results.txt
echo "TEST: demos/spring-boot/original" >> demo-test-results.txt
echo "==========================================" >> demo-test-results.txt
java -Dfile.encoding=UTF-8 -jar demos/spring-boot/original/target/spring-boot-original-1.0.0.jar 2>&1 | grep -E "(Started|users)" >> demo-test-results.txt &
SB_ORIG_PID=$!
sleep 8
kill $SB_ORIG_PID 2>/dev/null
echo "" >> demo-test-results.txt
echo "--- END: spring-boot/original ---" >> demo-test-results.txt
echo "" >> demo-test-results.txt

echo "Spring Boot demos测试完成"
