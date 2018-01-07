package org.authlab.util

import java.util.concurrent.ThreadLocalRandom

fun randomPort() = ThreadLocalRandom.current().nextInt(1024, 65535)
