package com.romafrihet.server.subject.dto

import com.romafrihet.server.subject.SchoolLevel

data class SubjectDto(val id: Long, val schoolLevel: SchoolLevel, val subjectGroup: String, val name: String)
