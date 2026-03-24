package com.evmap.serverapp.features.report.application

class ReportNotFoundException(id: Long) : RuntimeException("Report $id not found")
