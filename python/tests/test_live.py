"""실제 세종 포털 연동 (환경변수 SEJONG_TEST_STUDENT_ID, SEJONG_TEST_PASSWORD 있을 때만)"""

import os

import pytest

from sejongpeia import ErrorCode, SejongAuthError, authenticate, authenticate_dhc, authenticate_sis

STUDENT_ID = os.environ.get("SEJONG_TEST_STUDENT_ID")
PASSWORD = os.environ.get("SEJONG_TEST_PASSWORD")

pytestmark = pytest.mark.skipif(not (STUDENT_ID and PASSWORD), reason="SEJONG_TEST_* not set")


def test_live_integrated():
    result = authenticate(STUDENT_ID, PASSWORD)
    assert result.student_id == STUDENT_ID
    assert result.major and result.name
    assert result.classic_reading.certifications
    assert result.email is not None


def test_live_dhc_and_sis():
    assert authenticate_dhc(STUDENT_ID, PASSWORD).student_id == STUDENT_ID
    assert authenticate_sis(STUDENT_ID, PASSWORD).student_id == STUDENT_ID


def test_live_wrong_password():
    with pytest.raises(SejongAuthError) as error:
        authenticate_dhc(STUDENT_ID, PASSWORD + "-wrong")
    assert error.value.code == ErrorCode.AUTHENTICATION_FAILED
