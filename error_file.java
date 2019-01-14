
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * cond 是一个hashmap
 * 如果value 类型是int ，就判断为执行= 操作
 * 如果value 类型是String，就判断执行like 操作
 * <p>
 * <p>
 * <p>
 * operations:
 * like  : string contains
 * sw    : string startwiths
 * ew    : string endwiths
 * in    : multi equals
 * ni    : not multi equals
 * eq    : equals
 * ne    : not equals
 * datebtw: date between
 * btw   : between
 * lt    : less than
 * le    : less than or equal to
 * gt    : great than
 * ge    : great than or equal to
 *
 * @author: caiqyxyx
 * @date: 2018/10/8 16:24
 */
public class SimpleSpecification<T> implements Specification<T> {
    Logger logger = LoggerFactory.getLogger(SimpleSpecification.class);

    public enum Ops {
        LIKE("like"),
        // begin with
        SW("sw"),
        // end with
        EW("ew"),
        IN("in"),
        NI("ni"),
        EQ("eq"),
        NE("ne"),
        BTW("btw"),
        DATEBTW("datebtw"),
        LT("lt"),
        LE("le"),
        GT("gt"),
        GE("ge");

        private String op;

        Ops(String op) {
            this.op = op;
        }

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public static Ops opOf(String key) {
            return Ops.valueOf(key.toUpperCase());
        }

        public static String getKey(String key) {
            if (StringUtils.isEmpty(key))
                return null;
            else {
                if (key.indexOf('(') == -1 || key.indexOf(')') == -1) {
                    return key;
                } else {
                    return key.substring(key.indexOf('(') + 1, key.indexOf(')'));
                }
            }
        }

        public static String getOp(String key) {
            if (StringUtils.isEmpty(key))
                return null;
            else {
                if (key.indexOf('(') == -1 || key.indexOf(')') == -1) {
                    return null;
                } else {
                    return key.substring(0, key.indexOf('('));
                }
            }
        }

    }

    private Map<String, Object> cond = new HashMap<>();

    public SimpleSpecification() {
    }

    public SimpleSpecification(Map<String, Object> cond) {
        this.cond = cond;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> list = new ArrayList<Predicate>();
        if (cond != null) {
            cond.forEach((key, val) -> {
                Predicate o = box(key, val, root, criteriaBuilder);
                if (null != o) {
                    list.add(o);
                }
            });
        }
        Predicate[] p = new Predicate[list.size()];
        return criteriaBuilder.and(list.toArray(p));
    }

    private Predicate box(String key, Object val, Root<T> root, CriteriaBuilder criteriaBuilder) {
        if (StringUtils.isEmpty(key) || null == val) return null;
        if (key.indexOf('(') == -1 && key.indexOf(')') == -1) {
            // simple op
            if (val instanceof String) {
                return criteriaBuilder.like(root.get(key).as(String.class), "%" + val + "%");
            } else if (val instanceof Integer) {
                return criteriaBuilder.equal(root.get(key).as(Integer.class), val);
            } else if (val instanceof ArrayList) {
                ArrayList arrayList = (ArrayList) val;
                if (arrayList.size() > 0) {
                    CriteriaBuilder.In<Object> in = criteriaBuilder.in(root.get(key));
                    for (Object o : (ArrayList) val) {
                        in.value(o);
                    }
                    return in;
                }
            } else if (val instanceof Boolean) {
                return criteriaBuilder.equal(root.get(key).as(Boolean.class), val);
            } else {
                return null;
            }
            return null;
        } else {
            // complex op
            String op = Ops.getOp(key);
            String parsedKey = Ops.getKey(key);

            if (null == op || parsedKey == null) {
                return null;
            }

            Ops ops = Ops.opOf(op);

            switch (ops) {
                case SW:
                    return criteriaBuilder.like(root.get(parsedKey).as(String.class), val.toString() + "%");
                case EQ:
                    return criteriaBuilder.equal(root.get(parsedKey), val);
                case EW:
                    return criteriaBuilder.like(root.get(parsedKey).as(String.class), "%" + val.toString());
                case GE:
                    return criteriaBuilder.ge(root.get(parsedKey), new Integer(val.toString()));
                case GT:
                    return criteriaBuilder.gt(root.get(parsedKey), new Integer(val.toString()));
                case IN:
                    CriteriaBuilder.In<Object> in = criteriaBuilder.in(root.get(parsedKey));
                    for (Object o : (ArrayList) val) {
                        in.value(o);
                    }
                    return in;
                case LE:
                    return criteriaBuilder.le(root.get(parsedKey), new Integer(val.toString()));
                case LT:
                    return criteriaBuilder.lt(root.get(parsedKey), new Integer(val.toString()));
                case NE:
                    return criteriaBuilder.notEqual(root.get(parsedKey), val);
                case NI:
                    CriteriaBuilder.In<Object> ni = criteriaBuilder.in(root.get(parsedKey));
                    for (Object o : (ArrayList) val) {
                        ni.value(o);
                    }
                    return ni.not();
                case DATEBTW:
                    ArrayList list = (ArrayList) val;
                    SimpleDateFormat simpleDateFormat = null;
                    if (list.size() < 3) {
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    } else {
                        String format = ((ArrayList) val).get(2).toString();
                        simpleDateFormat = new SimpleDateFormat(format);
                    }
                    try {
                        Date start = simpleDateFormat.parse(((ArrayList) val).get(0).toString());
                        Date end = simpleDateFormat.parse(((ArrayList) val).get(1).toString());
                        return criteriaBuilder.between(root.get(parsedKey), start, end);
                    } catch (ParseException e) {
                        logger.error(e.getMessage());
                        return null;
                    }
                case BTW:
                    try {
                        Integer start = Integer.parseInt(((ArrayList) val).get(0).toString());
                        Integer end = Integer.parseInt(((ArrayList) val).get(1).toString());
                        return criteriaBuilder.between(root.get(parsedKey), start, end);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return null;
                    }
                case LIKE:
                    return criteriaBuilder.like(root.get(parsedKey).as(String.class), "%" + val.toString() + "%");
                default:
                    return null;
            }

        }
    }
}
