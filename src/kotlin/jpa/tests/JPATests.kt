package kotlin.jpa.tests

import javax.persistence.*
import kotlin.properties.Delegates
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import java.util.ArrayList
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import org.joda.time.DateTimeZone
import org.joda.time.MonthDay
import kotlin.test.assertEquals
import org.junit.After
import java.io.Serializable
import kotlin.jpa.JPAEntity
import kotlin.jpa.JPAEntityClass
import kotlin.jpa.JPASession

/**
 * Created by maxfeldman on 21/04/14.
 */

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Persistent Model

Entity Table(name="ACCOUNTS")
public class Account : JPAEntity {
    class object: JPAEntityClass<Account>()
    Id GeneratedValue(strategy = GenerationType.AUTO) Column(name = "id", updatable = false, nullable = false)
    var id:Long=0
    Version Column(name="OPTLOCK")
    val version:Int=0
    Column
    var name:String=""
    OrderBy("date") OneToMany(mappedBy="accountFrom", orphanRemoval=true, cascade=array(CascadeType.ALL), fetch=FetchType.EAGER)
    var transactionFrom:MutableList<Transaction> = ArrayList<Transaction>()
}

Entity Table(name="TRANSACTIONS") public class Transaction : JPAEntity {
    class object: JPAEntityClass<Transaction>()
    Id GeneratedValue(strategy = GenerationType.AUTO) Column(name = "id", updatable = false, nullable = false)
    var id:Long=0
    Version Column(name="OPTLOCK")
    val version:Int=0
    Column Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    var date:DateTime = DateTime.now()!!
    ManyToOne JoinColumn(name="accountFrom_fk")
    var accountFrom:Account?=null
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Tests

class JPATests() {
    Before fun setUp(){
        with(JPASession(), {
            val a:Account = Account()
            a.name = "Супернейм"
            transactional { em persist a }

            var t1 = Transaction()
            Thread.sleep(10)
            var t2 = Transaction()
            t1.accountFrom = a; t2.accountFrom = a
            transactional {
                em persist t1; em persist t2
            }
        })
    }

    After fun tearDown() {
        with(JPASession(), {
            transactional {
                val a = Account.findByName("Супернейм")
                if (a != null) em remove a
            }
        })
    }

    Test fun testUnicode() {
        with(JPASession(), {
            val a:Account = Account.findByName("Супернейм")!!
            assertEquals("Супернейм" , a.name, "Unicode characters read incorrectly")
        })
    }

    Test fun testBidirectionalReferenceSymmetry() {
        with(JPASession(), {
            val a:Account = Account.findByName("Супернейм")!!
            assertEquals(2, a.transactionFrom.size(), "'Many' collection should contain two elements." )
        })
    }

    Test fun testOrphanRemoval() {
        with(JPASession(), {
            val a:Account = Account.findByName("Супернейм")!!
            a.transactionFrom remove 0
            transactional { em persist a }
            em refresh a
            assertEquals(1, a.transactionFrom.size(), "'Many' collection should contain only one elements." )
        })
    }

    Test fun testCascadedRemoval() {
        with(JPASession(), {
            transactional { em remove Account.findByName("Супернейм") }
            assertEquals(0, Transaction.all().size, "'Many' collection should be empty by now")
        })
    }
}