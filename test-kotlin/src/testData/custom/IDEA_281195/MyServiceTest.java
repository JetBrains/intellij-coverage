/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testData.custom.IDEA_281195;

import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class MyServiceTest {
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Tested(fullyInitialized = true)
  MyService businessService;
  @Mocked
  MyService.SimpleEmail anyEmail;

  @Test
  public void testBusinessOperationXyz() {
    businessService.doBusinessOperationXyz();

    new Verifications() {{
      anyEmail.send();
      times = 1;
    }};
  }
}