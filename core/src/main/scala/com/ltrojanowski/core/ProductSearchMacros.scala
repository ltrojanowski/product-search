package com.ltrojanowski.core

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.reflect.macros.blackbox

trait ProductSearch[A, B] {
  def find(a: A): B
}

object ProductSearch {
  implicit def materializeProductSearch[A, B]: ProductSearch[A, B] =
    macro ProductSearchMacros.find_impl[A, B]

}

object ProductSearchMacros {

  private def isProductSymbol(symbol: String): Boolean = {
    val isProductSymbolPattern = "^scala.Product(\\d{1,2})$".r
    symbol match {
      case isProductSymbolPattern(size) if Integer.valueOf(size) < 23 => true
      case _                                                          => false
    }
  }

  def find_impl[A: c.WeakTypeTag, B: c.WeakTypeTag](
      c: blackbox.Context
  ): c.Expr[ProductSearch[A, B]] = {
    import c.universe._
    val bType: Type = weakTypeOf[B]
    val aType: Type = weakTypeOf[A]

    def typeIsProduct(tpe: Type): Boolean = {
      tpe.baseClasses.exists(c => isProductSymbol(tpe.baseType(c).typeSymbol.fullName))
    }

    if (!typeIsProduct(aType)) {
      sys.error(
        s"ProductSearch.find macro can only be used on products, this is a: ${aType.typeSymbol.fullName}"
      )
    }

    case class Node(idx: Int, tpe: Type) {
      def children: Vector[Node] = tpe.dealias.typeArgs.zipWithIndex.map { case (t, i) =>
        Node(i, t)
      }.toVector
      def isTuple: Boolean = typeIsProduct(tpe) // isProductSymbol(tpe.typeSymbol.fullName)
    }

    @tailrec
    def moveToSiblingOrBacktrack(
        curNode: Node,
        prevNodes: List[Node]
    ): (Node, List[Node]) = {
      prevNodes.headOption match {
        case None =>
          sys.error(
            s"This product does not contain the searched for type: ${bType.typeSymbol.fullName}"
          )
        case Some(parent) => {
          try {
            (parent.children(curNode.idx + 1), prevNodes)
          } catch {
            case e: IndexOutOfBoundsException => {
              val tail = prevNodes.tail
              val newCurrent = prevNodes.head
              moveToSiblingOrBacktrack(newCurrent, tail)
            }
          }
        }
      }
    }

    @tailrec
    def findTypeInNestedTuples(
        currentNode: Node,
        previousNodes: List[Node] = Nil
    ): List[String] = {
      if (currentNode.tpe == bType) {
        (currentNode :: previousNodes).reverse.tail.map(n => s"_${n.idx + 1}")
      } else if (currentNode.isTuple) {
        // check if tuple and if it is go deeper
        findTypeInNestedTuples(
          currentNode.children.head,
          currentNode :: previousNodes
        )
      } else {
        // is not tuple and not searched for value, so in parent check next after current node
        // if next after current node does not exist then backtrack
        // if no parent exists then the value does not exist in this tuple
        val (newCur, newPrev) =
          moveToSiblingOrBacktrack(currentNode, previousNodes)
        findTypeInNestedTuples(newCur, newPrev)
      }
    }
    val findImpl =
      findTypeInNestedTuples(Node(0, aType))
        .foldLeft(Ident(TermName("a")): Tree) { case (tree, term) =>
          Select(tree, TermName(term))
        }

    reify {
      new ProductSearch[A, B] {
        def find(a: A): B = c.Expr[B](findImpl).splice
      }
    }
  }
}
