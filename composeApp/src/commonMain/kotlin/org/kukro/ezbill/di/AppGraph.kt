package org.kukro.ezbill.di

import org.kukro.ezbill.app.AppRootStateMachine
import org.kukro.ezbill.data.datasource.AppSessionDataSource
import org.kukro.ezbill.data.datasource.SupabaseAccountDataSource
import org.kukro.ezbill.data.datasource.SupabaseExpenseDataSource
import org.kukro.ezbill.data.repository.DefaultAccountRepository
import org.kukro.ezbill.data.repository.DefaultExpenseRepository
import org.kukro.ezbill.data.repository.DefaultSessionRepository
import org.kukro.ezbill.domain.usecase.AccountUseCases
import org.kukro.ezbill.domain.usecase.ExpenseUseCases
import org.kukro.ezbill.domain.usecase.SessionUseCases

object AppGraph {
    private val sessionRepository = DefaultSessionRepository(AppSessionDataSource)
    private val accountRepository = DefaultAccountRepository(SupabaseAccountDataSource)
    private val expenseRepository = DefaultExpenseRepository(SupabaseExpenseDataSource)

    val sessionUseCases = SessionUseCases(sessionRepository)
    val accountUseCases = AccountUseCases(accountRepository, sessionRepository)
    val expenseUseCases = ExpenseUseCases(expenseRepository)

    val rootStateMachine = AppRootStateMachine(sessionUseCases)
}

