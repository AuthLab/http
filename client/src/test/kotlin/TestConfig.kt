import io.kotlintest.ProjectConfig
import org.authlab.crypto.setupDefaultSslContext

object TestConfig : ProjectConfig() {
    override fun beforeAll() {
        setupDefaultSslContext()
    }
}