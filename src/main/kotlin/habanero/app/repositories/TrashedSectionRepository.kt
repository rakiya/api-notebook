package habanero.app.repositories

import habanero.extensions.database.Tables
import habanero.extensions.database.tables.Page
import habanero.extensions.database.tables.Section
import habanero.extensions.database.tables.records.SectionRecord
import org.jooq.DSLContext

/**
 * ゴミ箱にあるセクションについてのリポジトリ
 *
 * @author Ryutaro Akiya
 */
class TrashedSectionRepository(tx: DSLContext) : Repository<SectionRecord>(tx) {

    /**
     * アカウントownerIdが所有する、ゴミ箱にあるセクションを全て取得する。
     *
     * @param ownerId String セクション所有者のアカウントID
     * @return List<SectionRecord> ゴミ箱にあるセクション
     */
    fun getAllOwnedBy(ownerId: String): List<SectionRecord> {
        return tx.select()
                .from(Section.SECTION)
                .innerJoin(Page.PAGE).using(Page.PAGE.PAGE_ID)
                .innerJoin(Tables.NOTEBOOK).using(Tables.NOTEBOOK.NOTEBOOK_ID)
                .where(Tables.NOTEBOOK.ACCOUNT_ID.eq(ownerId))
                .and(Section.SECTION.IS_TRASHED.eq(1))
                .fetchInto(Section.SECTION)
    }
}