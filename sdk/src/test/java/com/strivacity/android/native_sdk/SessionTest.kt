package com.strivacity.android.native_sdk

import TokenResponseBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class SessionTest {

  private lateinit var storage: TestStorage

  @Before
  fun setUp() {
    storage = TestStorage()
  }

  @Test
  fun loadingState_shouldDefaultToFalse() = runTest {
    val session = Session(storage, mock())
    assertFalse(session.loginInProgress.first())
  }

  @Test
  fun loadingState_shouldEmitNewState() = runTest {
    val session = Session(storage, mock())
    session.setLoginInProgress(true)
    assertTrue(session.loginInProgress.first())
  }

  @Test
  fun load_shouldEmitNewProfile_whenProfileIsStored() {
    val testTokenResponse = TokenResponseBuilder().buildAsString()
    val profile = Profile(Json.decodeFromString(testTokenResponse))
    val encodedProfile = Json.encodeToString(profile)

    val mockStorage = mock<Storage> { on { get(any()) } doReturn encodedProfile }

    val session = Session(mockStorage, mock())
    assertNull(session.profile.value)
    session.load()
    assertNotNull(session.profile.value)

    verify(mockStorage).get("profile")
    verifyNoMoreInteractions(mockStorage)
  }

  @Test
  fun load_shouldNotEmit_whenProfileIsMissing() {
    val mockStorage = mock<Storage> { on { get(any()) } doReturn null }
    val session = Session(mockStorage, mock())
    session.load()
    assertNull(session.profile.value)

    verify(mockStorage).get("profile")
    verifyNoMoreInteractions(mockStorage)
  }

  @Test
  fun clear_shouldRestoreInitialState() {
    val mockStorage = mock<Storage>()
    val session =
        Session(mockStorage, mock()).apply {
          setLoginInProgress(true)
          update(TokenResponseBuilder().createAsTokenResponse())
        }
    verify(mockStorage).set(argThat { equals("profile") }, any<String>())

    // then
    session.clear()

    // assert
    assertNull(session.profile.value)
    assertFalse(session.loginInProgress.value)

    verify(mockStorage).delete("profile")
    verifyNoMoreInteractions(mockStorage)
  }
}

class TestStorage(val backing: MutableMap<String, String> = mutableMapOf()) : Storage {
  override fun set(key: String, value: String): Boolean {
    backing[key] = value
    return true
  }

  override fun get(key: String): String? = backing[key]

  override fun delete(key: String) {
    backing.remove(key)
  }
}
