#!/bin/bash

#
# Copyright 2000-2024 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TEST_CASE="$1"
MODULE="$2"

if [ -z "$MODULE" ]; then
  MODULE="test-sources"
fi

if [ -z "$TEST_CASE" ]; then
  echo "No test case provided"
  exit 1
fi

SOURCE="kotlin"

if [ "$SOURCE" = "kotlin" ]; then
    TEST_FILE="test.kt"
else
    TEST_FILE="Test.java"
fi

TEST_CASE="${TEST_CASE//.//}"
echo "Test case: file://$(pwd)/${MODULE}/src/testData/${TEST_CASE}/$TEST_FILE"

../gradlew :test-kotlin:classes > /dev/null || exit 2

javap -v -l -p "${MODULE}"/build/classes/"$SOURCE"/main/testData/"$TEST_CASE"/*.class > bytecode.txt || exit 3
echo "file://$(pwd)"/bytecode.txt
