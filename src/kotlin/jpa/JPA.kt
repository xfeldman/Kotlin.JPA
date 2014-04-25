package kotlin.jpa

import java.io.Serializable
import kotlin.properties.Delegates
import java.util.ArrayList
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

/**
 * Created by maxfeldman on 25/04/14.
 */
public object JPA {
    val entityManagerFactory: EntityManagerFactory by Delegates.lazy { Persistence.createEntityManagerFactory("mysql-db")!! }
}

trait JPAEntity:Serializable

open class JPAEntityClass<out T:JPAEntity> {
    val klass = javaClass.getEnclosingClass()!!.getSimpleName()
}

class JPASession {
    val em by Delegates.lazy { JPA.entityManagerFactory.createEntityManager()!! }
    val transaction by Delegates.lazy { em.getTransaction()!!}

    inline fun transactional(f:JPASession.() -> Unit) {
        transaction.begin()
        f()
        transaction.commit()
    }

    public fun <E: JPAEntity,T: JPAEntityClass<out E>> T.findByName(name:String) : E? {
        return em.createQuery("SELECT a FROM ${klass} a WHERE a.name LIKE :custName")?.setParameter("custName", name)?.getResultList()?.first as E?
    }

    public fun <E: JPAEntity,T: JPAEntityClass<out E>> T.all() : MutableList<E> {
        return em.createQuery("SELECT a FROM ${klass} a")!!.getResultList() as MutableList<E> ?: ArrayList<E>()
    }

    fun finally() {
        em.close()
    }
}
