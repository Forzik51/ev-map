package com.evmap.serverapp.features.report.application

class InvalidReportException(message: String) : RuntimeException(message)

class ReportConflictException(message: String) : RuntimeException(message)
